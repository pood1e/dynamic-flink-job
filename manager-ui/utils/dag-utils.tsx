'use client'

import {analyzeFunctionRelations, analyzeLevels} from "@/utils/analyze-utils";
import {Graph} from "@antv/x6";
import './function-graph.css'
import { v4 as uuidv4 } from 'uuid';


export function parseJobGraph(data: any) {
    const nodes: any = data.functions ? data.functions.map((fun: any) => {
        return initFunction2Node(fun, false)
    }) : []

    const edges: any = data.pipes ? data.pipes.map((pipe: any) => {
        return {
            shape: 'edge',
            connector: 'smooth',
            data: {
                ...pipe
            },
            source: {
                cell: pipe.source,
                port: `${pipe.source}-right`
            },
            target: {
                cell: pipe.target,
                port: `${pipe.target}-left`
            },
            labels: getEdgeLabels(pipe),
            attrs: {
                line: {
                    strokeDasharray: 5,
                    stroke: '#3471F9',
                    style: {
                        animation: "running-line 30s infinite linear"
                    }
                }
            }
        }
    }) : []

    const map = analyzeFunctionRelations(data.pipes)
    const levelOrder: Array<Array<string>> = analyzeLevels(map)
    if (levelOrder.length > 0) {
        const nodeMap: Map<string, any> = new Map(nodes.map((value: any) => [value.id, value]));
        for (let x = 0; x < levelOrder.length; x++) {
            const xPos = x * 350
            for (let y = 0; y < levelOrder[x].length; y++) {
                const yPos = y * 150
                const node = nodeMap.get(levelOrder[x][y])
                node['x'] = xPos
                node['y'] = yPos
            }
        }
    }

    return {
        nodes: nodes,
        edges: edges
    }
}

export function addNewFunction(graph: Graph, func: any) {
    const newNode: any = initFunction2Node(func, true)
    newNode['x'] = 300
    newNode['y'] = 300
    graph.addNode(newNode)
    graph.centerContent()
}

export function initFunction2Node(func: any, isNew: boolean) {
    const id = isNew ? uuidv4() : func.id
    const ports = []
    if (func.type === "SINK" || func.type === "PROCESSOR" || func.type === "AGGREGATOR") {
        ports.push({
            id: `${id}-left`,
            group: 'left'
        })
    }
    if (func.type === "SOURCE" || func.type === "PROCESSOR" || func.type === "AGGREGATOR") {
        ports.push({
            id: `${id}-right`,
            group: 'right'
        })
    }
    return {
        shape: `FUNCTION-${func.type}`,
        id: id,
        data: {
            id: id,
            ...func,
            isNew: isNew
        },
        ports: ports
    }
}

export function getEdgeLabels(pipe: any) {
    let labels = []
    if (pipe.broadcast) {
        labels.push({
            attrs: {
                label: {
                    text: "Broadcast"
                }
            },
            position: 0.4
        })
    }
    if (pipe.key) {
        labels.push({
            attrs: {
                label: {
                    text: `SideOutput(${pipe.key})`
                }
            },
            position: 0.6
        })
    }
    return labels
}

