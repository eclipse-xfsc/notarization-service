import os
import subprocess
import logging
from dotenv import load_dotenv

def runCommand(args):
    if isinstance(args, str):
        args = args.split(' ')
    return subprocess.run(args, stdout=subprocess.PIPE).stdout.decode('utf-8')

def logMessage(message):
    os.system("echo " + message)

def logError(message, err):
    logging.exception(message)

def suche_helm_ordner(root):
    helm_ordner = []
    
    for root, dirs, files in os.walk(root):
        if "helm" in dirs: 
         helm_ordner.append(os.path.join(root,"helm"))
    
    return helm_ordner


def push_tgz_files(root):
    host=os.environ.get('HARBOR_HOST')
    project=os.environ.get('HARBOR_PROJECT')
    user=os.environ.get('HARBOR_USERNAME')
    pwd=os.environ.get('HARBOR_PASSWORD')
    dryRun = os.environ.get('DRY_RUN')
    hostname = str(host)+"/"+str(project)
    if dryRun:
        logMessage('Performing dry run. Will not push to Harbor registry.')
        result = 0
    else:
        logMessage("Start Login of registry: " + str(hostname))
        result = os.system("helm registry login "+hostname+ " -u '"+user+"' -p '"+ pwd+"'")
   
    if result == 0:
        if (host == None or project == None) and not dryRun:
            logMessage("Invalid Harbor Project")
            os._exit(1)
        
        for root, dirs, files in os.walk(root):
            for file in files:
                if file.endswith(".tgz"): 
                    file = os.path.join(root,file)
                    logMessage("start pushing to ... " + "oci://" + hostname + " the file: " + str(file))
                    if not dryRun:
                        os.system("helm push "+ file + " oci://" + hostname) 
    else:
        logMessage("Authorization for Harbor failed.")
        os._exit(1)
    if not dryRun:
        os.system("helm registry logout "+hostname)   


if __name__ == "__main__":
    root = "."
    load_dotenv()
    folders = suche_helm_ordner(root)
    versionTag=None
    tag=os.environ.get("CI_COMMIT_TAG")
    branch=os.environ.get("CI_COMMIT_BRANCH")

    if tag == None: 
        if branch != None:
            versionTag=branch
    else:
        versionTag=tag
    if not versionTag or versionTag == None:
        versionTag = runCommand('git rev-parse --abbrev-ref HEAD')
    logMessage("Version tag: " + str(versionTag))

    if folders:
        os.system("mkdir /tmp/helmcharts")
        for baseDir in folders:
            for helm_ordner, dirs, files in os.walk(baseDir):
                
                if "Chart.yaml" in files:
                    logMessage('Checking directory:' + str(helm_ordner))     
                    try:
                        path = os.path.join(helm_ordner, "Chart.yaml")

                        with open(path, 'r') as file:
                            content = file.read()
                            
                        new_content = content.replace("-tag", "-"+versionTag)

                        with open(path, 'w') as file:
                            file.write(new_content)
                    except FileNotFoundError:
                        logMessage('Chart.yaml not found')
                        os._exit(1)
                    except Exception as e:
                        logError('Error:', e)
                        os._exit(1) 
                            
                    os.system("helm dependency build "+helm_ordner)
                    os.system("helm package "+helm_ordner +" -d /tmp/helmcharts/" + str(helm_ordner))    
        push_tgz_files("/tmp/helmcharts")  
    else:
        logMessage("No Folders found.")
