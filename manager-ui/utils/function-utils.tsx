'use client'

import {Graph, Node} from "@antv/x6";
import {Button, Card, Flex, Form, Input, Select, Switch, Tag} from "antd";
import {register} from "@antv/x6-react-shape";
import React, {useEffect, useState} from "react";
import {addFunction, deleteFunction, getPluginClasses, updateFunction, updateJob} from "@/client/client-api";
import {getEdgeLabels} from "@/utils/dag-utils";
import {DeleteOutlined} from "@ant-design/icons";

const types: any = {
    "SOURCE": {
        tagColor: "success"
    },
    "PROCESSOR": {
        tagColor: "processing"
    },
    "SINK": {
        tagColor: "warning"
    },
    "AGGREGATOR": {
        tagColor: "error"
    }
}

export function registryDagShape() {
    const portAttr = {
        attrs: {
            circle: {
                magnet: true,
                stroke: '#8f8f8f',
                r: 5,
            },
        }
    }
    const right = {
        right: {
            position: "right",
            ...portAttr
        }
    }
    const left = {
        left: {
            position: "left",
            ...portAttr
        }
    }
    const basic = {
        width: 200,
        height: 60,
        effect: ['data'],
        component: FunctionGraph,
    }
    register({
        shape: "FUNCTION-SOURCE",
        ...basic,
        ports: {
            groups: {
                ...right
            }
        }
    })
    register({
        shape: "FUNCTION-PROCESSOR",
        ...basic,
        width: 250,
        ports: {
            groups: {
                ...right,
                ...left
            }
        }
    })
    register({
        shape: "FUNCTION-AGGREGATOR",
        ...basic,
        width: 250,
        ports: {
            groups: {
                ...right,
                ...left
            }
        }
    })
    register({
        shape: "FUNCTION-SINK",
        ...basic,
        ports: {
            groups: {
                ...left
            }
        }
    })
}

const setValue = (value: any, fn: Function) => {
    fn(value)
}

export function FunctionModifyPanel({cell, closeDrawer}: { cell: any, closeDrawer: Function }) {
    const func = cell.getData()
    const [name, setName] = useState()
    const [parallelism, setParallelism] = useState()
    const [impl, setImpl] = useState()
    const [keyBy, setKeyBy] = useState()
    const [boundedOutOfOrder, setBoundedOutOfOrder] = useState()
    const [windowSeconds, setWindowSeconds] = useState()
    const [availableImpls, setAvailableImpls] = useState<Array<any>>([])
    const [fnConfig, setFnConfig] = useState<any>()

    useEffect(() => {
        setName(func.name)
        setParallelism(func.parallelism)
        setImpl(func.impl)
        setFnConfig(JSON.stringify(func.config, null, 2))
        setKeyBy(func.keyBy)
        setBoundedOutOfOrder(func.boundedOutOfOrder)
        setWindowSeconds(func.windowSeconds)
        let unmount = false
        getPluginClasses().then(res => {
            if (!unmount && res) {
                setAvailableImpls(res.filter((c: any) => c.type === func.type))
            }
        })
        return () => {
            unmount = true
        }
    }, [func]);

    const options = availableImpls.filter((value: any) => value.type).map((value: any) => {
        return {
            value: value.className,
            label: value.className
        }
    })

    const updateAction = () => {
        cell.setData({
            ...func,
            name: name,
            impl: impl,
            changed: true,
            keyBy: keyBy,
            boundedOutOfOrder: boundedOutOfOrder,
            windowSeconds: windowSeconds,
            parallelism: parallelism,
            config: JSON.parse(fnConfig)
        })
        closeDrawer()
    }

    let extraOption = null
    if (func.type === 'AGGREGATOR') {
        extraOption = (
            <>
                <Form.Item label="keyBy">
                    <Switch value={keyBy} onChange={value => setValue(value, setKeyBy)}/>
                </Form.Item>
                <Form.Item label="delaySeconds">
                    <Input type="number" value={boundedOutOfOrder}
                           onChange={e => setValue(e.target.value, setBoundedOutOfOrder)}/>
                </Form.Item>
                <Form.Item label="windowSeconds">
                    <Input type="number" value={windowSeconds}
                           onChange={e => setValue(e.target.value, setWindowSeconds)}/>
                </Form.Item>
            </>
        )
    }

    return (<Form labelCol={{span: 6}}>
        <Form.Item label="id">
            <Input disabled value={func.id}/>
        </Form.Item>
        <Form.Item label="type">
            <FunctionTag type={func.type}/>
        </Form.Item>
        <Form.Item label="name">
            <Input value={name} onChange={e => setValue(e.target.value, setName)}/>
        </Form.Item>
        <Form.Item label="parallelism">
            <Input type="number" value={parallelism}
                   onChange={e => setValue(e.target.value, setParallelism)}/>
        </Form.Item>
        <Form.Item label="impl">
            <Select value={impl} onChange={e => setImpl(e)} options={options}/>
        </Form.Item>
        {extraOption}
        <Form.Item label="config">
            <Input.TextArea rows={15} value={fnConfig}
                            onChange={e => setValue(e.target.value, setFnConfig)}/>
        </Form.Item>
        <Form.Item>
            <Flex align="center" justify="end" gap="middle">
                <Button onClick={() => {
                    cell.remove()
                    closeDrawer()
                }} danger icon={<DeleteOutlined/>}/>
                <Button onClick={updateAction} type="primary">update</Button>
                <Button onClick={() => closeDrawer()}>cancel</Button>
            </Flex>
        </Form.Item>
    </Form>)
}

export function EdgeModifyPanel({edge, closeDrawer}: { edge: any, closeDrawer: Function }) {
    const data = edge.getData()
    const [key, setKey] = useState(data.key ? data.key : '')
    const [broadcast, setBroadcast] = useState<boolean>(data.broadcast)

    const updateAction = () => {
        const newData = {
            source: edge.getSourceCellId(),
            target: edge.getTargetCellId(),
            broadcast: broadcast,
            key: key ? key : null
        }
        edge.setData(newData)
        edge.setLabels(getEdgeLabels(newData))
        edge.attr('line/strokeDasharray', 5)
        edge.attr('line/style/animation', 'running-line 30s infinite linear')
        closeDrawer()
    }

    return (
        <Form labelCol={{span: 6}}>
            <Form.Item label="source">
                <Input disabled value={edge.getSourceCellId()}/>
            </Form.Item>
            <Form.Item label="target">
                <Input disabled value={edge.getTargetCellId()}/>
            </Form.Item>
            <Form.Item label="side key">
                <Input value={key} onChange={e => setValue(e.target.value, setKey)}/>
            </Form.Item>
            <Form.Item label="broadcast">
                <Switch value={broadcast} onChange={value => setValue(value, setBroadcast)}/>
            </Form.Item>
            <Form.Item>
                <Flex align="center" justify="end" gap="middle">
                    <Button onClick={() => {
                        edge.remove()
                        closeDrawer()
                    }} danger icon={<DeleteOutlined/>}/>
                    <Button onClick={updateAction} type="primary">update</Button>
                    <Button onClick={() => closeDrawer()}>cancel</Button>
                </Flex>
            </Form.Item>
        </Form>
    )
}

export function FunctionTag({type}: { type: string }) {
    const tagColor = types[type].tagColor
    return <Tag color={tagColor}>{type}</Tag>
}

export function FunctionGraph({node, graph}: { node: Node, graph: Graph }) {
    const data = node.getData()

    const title = (<>
        <FunctionTag type={data.type}/>
        {data.isNew && <Tag>+</Tag>}
        <span>{data.name}</span>
    </>)
    return (<Card size="small" title={title}>
    </Card>)
}

export function parseSaveJob(job: any, cells: any, freshAction: Function) {
    const map = new Map()
    const functionIds = cells.filter((cell: any) => cell.shape !== 'edge').map((cell: any) => cell.id)
    const promises = cells.map((cell: any) => {
        if (cell.shape !== 'edge') {
            if (cell.data.isNew) {
                const obj = {
                    ...cell.data
                }
                delete obj['isNew']
                delete obj['changed']
                delete obj['id']

                const tmp = async () => {
                    const response = await addFunction(obj)
                    map.set(cell.id, response.id)
                }
                return tmp()
            } else if (cell.data.changed) {
                const obj = {
                    ...cell.data
                }
                delete obj['changed']
                return updateFunction(obj)
            }
        }
        return null
    }).filter((obj: any) => obj)
    const deletePromises = job.functions ? job.functions.map((func: any) => {
        if (functionIds.indexOf(func.id) < 0) {
            return deleteFunction(func.id)
        }
    }).filter((promise: any) => promise) : []
    const allPromises = promises.concat(deletePromises)

    Promise.all(allPromises).then(res => {
        const finalFunctions: any = []
        functionIds.forEach((functionId: any) => {
            const newId = map.get(functionId)
            if (newId) {
                finalFunctions.push(newId)
            } else {
                finalFunctions.push(functionId)
            }
        })
        const finalPipes = cells.filter((cell: any) => cell.shape === 'edge').map((cell: any) => {
            let source = map.get(cell.source.cell)
            if (!source) {
                source = cell.source.cell
            }
            let target = map.get(cell.target.cell)
            if (!target) {
                target = cell.target.cell
            }
            return {
                ...cell.data,
                source: source,
                target: target
            }
        })
        const updatedJob = {
            ...job,
            functions: finalFunctions,
            pipes: finalPipes
        }
        updateJob(updatedJob).then(res => {
            freshAction()
        })
    })
}

export {types}