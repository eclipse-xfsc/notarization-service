export function createStages(
  startUsers,
  incrementUserBy,
  duration,
  maxUsers = 1000,
  increasePerSec = 10
) {
    const stages = [];

    const initRampupTime = (startUsers / increasePerSec) * 1000;
    const initRampUp = { "duration": initRampupTime, "target": startUsers };
    const initStay = { "duration": duration, "target": startUsers };

    stages.push(initRampUp);
    stages.push(initStay);
  
    for (let currentUsers = startUsers + incrementUserBy; currentUsers <= maxUsers; currentUsers += incrementUserBy) {
        const rampUpStage = { "duration": "5s", "target": currentUsers};
        const stayStage = { "duration": duration, "target": currentUsers};

        stages.push(rampUpStage);
        stages.push(stayStage);
    }

    return stages;
}
