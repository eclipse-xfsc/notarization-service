 
echo "Removing empty declarations from PlantUML files. They prevent layouting by PlantUML itself."
sed -i '/^\w* : $/d' ./src-gen/*StateDiagram.puml
