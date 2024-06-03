export function analyzeFunctionRelations(pipes: Array<any>) {
    const map = new Map()
    pipes?.forEach(pipe => {
        let sourceNode = map.get(pipe.source)
        if (!sourceNode) {
            sourceNode = {
                id: pipe.source,
                next: [],
                prev: []
            }
            map.set(pipe.source, sourceNode)
        }
        sourceNode.next.push(pipe)

        let targetNode = map.get(pipe.target)
        if (!targetNode) {
            targetNode = {
                id: pipe.target,
                next: [],
                prev: []
            }
            map.set(pipe.target, targetNode)
        }
        targetNode.prev.push(pipe)
    });
    return map
}

function countLevels(relation: any, relationMap: Map<string, any>, levelCount: Map<string, number>) {
    let level = levelCount.get(relation.id)
    if (level != undefined && !isNaN(level)) {
        return level
    }
    if (relation.prev.length == 0) {
        levelCount.set(relation.id, 0)
        return 0
    }
    level = Math.max.apply(null, relation.prev.map((pipe: any) => {
        const prev = relationMap.get(pipe.source)
        return countLevels(prev, relationMap, levelCount)
    })) + 1
    levelCount.set(relation.id, level)
    return level
}

export function analyzeLevels(relationMap: Map<string, any>) {
    const levelCount = new Map()
    let maxLevel = 0
    relationMap.forEach((relation, _) => {
        if (relation.next.length != 0) {
            return
        }
        let level = countLevels(relation, relationMap, levelCount)
        if (level > maxLevel) {
            maxLevel = level
        }
    })
    if (maxLevel == 0) {
        return []
    }
    const levelArray = new Array(maxLevel + 1)
    levelCount.forEach((v, k) => {
        if (!levelArray[v]) {
            levelArray[v] = []
        }
        levelArray[v].push(k)
    })
    return levelArray
}

