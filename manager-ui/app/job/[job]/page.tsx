'use client'

import {Button, Drawer, Flex, FloatButton, Form, Input, Modal, Select} from "antd";
import {FullscreenExitOutlined, PlusOutlined, SaveOutlined} from "@ant-design/icons";
import React, {useEffect, useState} from "react";
import {getJobGraph, getPluginClasses} from "@/client/client-api";
import {
    EdgeModifyPanel,
    FunctionModifyPanel,
    FunctionTag,
    parseSaveJob,
    registryDagShape
} from "@/utils/function-utils";
import {Edge, Graph} from "@antv/x6";
import {Snapline} from "@antv/x6-plugin-snapline";
import {addNewFunction, parseJobGraph} from "@/utils/dag-utils";

function AddFunctionPanel({closeModal, addFunction}: {
    closeModal: Function,
    addFunction: Function
}) {
    const [name, setName] = useState<string>('')
    const [parallelism, setParallelism] = useState<number>(1)
    const [impl, setImpl] = useState<string | null>(null)
    const [availableImpls, setAvailableImpls] = useState<Array<any>>([])

    useEffect(() => {
        let unmount = false
        getPluginClasses().then(res => {
            if (!unmount) {
                const allFunctionClass = res.filter((value: any) => value.type)
                setAvailableImpls(allFunctionClass)
                if (allFunctionClass.length > 0) {
                    setImpl(allFunctionClass[0].className)
                }
            }
        })
        return () => {
            unmount = true
        }
    }, []);

    const options = availableImpls.map((value: any) => {
        return {
            value: value.className,
            label: <>
                <FunctionTag type={value.type}/>
                <span>{value.className}</span>
            </>
        }
    })

    const commitFunction = () => {
        addFunction({
            name: name,
            impl: impl,
            parallelism: parallelism,
            type: availableImpls.filter((s: any) => s.className === impl)[0].type
        })
        closeModal()
    }

    return (
        <Form labelCol={{span: 4}}>
            <Form.Item label="name">
                <Input value={name} onChange={e => setName(e.target.value)}></Input>
            </Form.Item>
            <Form.Item label="impl">
                <Select value={impl} onChange={e => setImpl(e)} options={options}/>
            </Form.Item>
            <Form.Item label="parallelism">
                <Input type="number" value={parallelism}
                       onChange={e => setParallelism(parseInt(e.target.value))}/>
            </Form.Item>
            <Form.Item>
                <Flex align="center" justify="end" gap="middle">
                    <Button onClick={commitFunction} type="primary">add</Button>
                    <Button onClick={() => closeModal()}>cancel</Button>
                </Flex>
            </Form.Item>
        </Form>
    )
}

export default function Page({params}: {
    params: {
        job: string
    }
}) {
    const [isModalOpen, setModalOpen] = useState(false)
    const [graph, setGraph] = useState<Graph>()
    const [changeData, setChangeData] = useState<any>(null)
    const [job, setJob] = useState()

    const freshJobGraph = () => {
        getJobGraph(params.job).then(res => {
            setJob(res)
            graph?.fromJSON(parseJobGraph(res))
            setTimeout(() => {
                graph?.centerContent()
            }, 100)
        })
    }

    useEffect(() => {
        let unmount = false
        const component = new Graph({
            async: false,
            container: document.getElementById('container') as HTMLElement,
            autoResize: true,
            background: {
                color: '#F2F7FA',
            },
            connecting: {
                allowBlank: false,
                allowLoop: false,
                allowNode: false,
                allowMulti: false,
                allowEdge: false
            }
        })
        component.use(new Snapline({
            enabled: true
        }))
        setGraph(component)
        registryDagShape()
        getJobGraph(params.job).then(res => {
            if (!unmount) {
                setJob(res)
                component.fromJSON(parseJobGraph(res))
                setTimeout(() => {
                    component.centerContent()
                }, 100)
            }
        })
        component.on('cell:click', ({e, x, y, cell, view}) => {
            setChangeData(cell)
        })
        component.on('edge:click', ({e, x, y, edge, view}) => {
            setChangeData(edge)
        })
        component.on('blank:click', () => {
            setChangeData(null)
        })
        component.on('edge:added', ({edge}) => {
            edge.setConnector('smooth')
            edge.setData({})
        })
        return () => {
            unmount = true
        }
    }, [params]);

    const closeModal = () => {
        setModalOpen(false)
    }

    const addFunction = (func: any) => {
        if (graph) {
            addNewFunction(graph, func)
        }
    }

    const save = () => {
        parseSaveJob(job, graph?.toJSON().cells, freshJobGraph)
        // console.log(graph?.toJSON())
    }

    let changePanel = null

    if (changeData) {
        if (changeData instanceof Edge) {
            changePanel = <EdgeModifyPanel closeDrawer={() => setChangeData(null)} edge={changeData}/>
        } else {
            changePanel = <FunctionModifyPanel closeDrawer={() => setChangeData(null)} cell={changeData}/>
        }
    }

    return (
        <>
            <div style={{width: '100%', height: '100%'}}>
                <div id="container"></div>
            </div>
            <Drawer open={!!changeData} onClose={() => setChangeData(null)} closeIcon={false} mask={false}
                    destroyOnClose width={600}>
                {changePanel}
            </Drawer>
            <Modal open={isModalOpen} footer={null} closeIcon={false} destroyOnClose>
                <AddFunctionPanel addFunction={addFunction} closeModal={closeModal}/>
            </Modal>
            <FloatButton.Group>
                <FloatButton icon={<FullscreenExitOutlined/>} onClick={() => graph?.centerContent()}/>
                <FloatButton icon={<SaveOutlined/>} onClick={() => save()}/>
                <FloatButton icon={<PlusOutlined/>} onClick={() => setModalOpen(true)}/>
            </FloatButton.Group>
        </>
    )
}