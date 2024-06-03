'use client'

import {Button, Card, Flex, FloatButton, Form, Input, message, Modal, Typography} from "antd";
import Icon, {DeleteOutlined, PlusOutlined} from "@ant-design/icons";
import JobSvg from "@/public/job.svg";
import React, {useEffect, useState} from "react";
import {useRouter} from "next/navigation";
import {addJob, deleteJob, getJobs} from "@/client/client-api";

function JobPanel({job, onDeleted, messageApi}: { job: any, onDeleted: Function, messageApi: any }) {
    const router = useRouter()
    const deleteAction = () => {
        deleteJob(job.id).then(res => {
            if (res === 200) {
                onDeleted()
                messageApi.success("delete successfully.");
            } else {
                messageApi.error("delete failed")
            }
        })
    }
    const deleteIcon = <Button icon={<DeleteOutlined/>} onClick={deleteAction} danger/>
    const titleNode = (
        <Flex align="center" gap="small">
            <Icon component={JobSvg} style={{fontSize: '1.5rem'}}/>
            <span>{job.name}</span>
        </Flex>
    )
    return (
        <Card key={job.id} title={titleNode} extra={deleteIcon} onClick={() => router.push("/job/" + job.id)} hoverable>
            <Typography.Paragraph type="secondary">{job.id}</Typography.Paragraph>
            <Typography.Paragraph>functions: {job.functions ? job.functions.length : 0}</Typography.Paragraph>
            <Typography.Paragraph>sync period: {job.syncPeriod}</Typography.Paragraph>
        </Card>
    )
}

function JobModal({closeModal}: { closeModal: Function }) {
    const [name, setName] = useState('')
    const [syncPeriod, setSyncPeriod] = useState(0)
    const [updating, setUpdating] = useState(false);


    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setName(e.target.value);
    }

    const handleSyncChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setSyncPeriod(parseInt(e.target.value));
    }

    const addAction = () => {
        setUpdating(true)
        addJob({
            name: name,
            syncPeriod: syncPeriod
        }).then(res => {
            setUpdating(false)
            closeModal(true)
        })
    }

    return (
        <Form labelCol={{span: 6}}>
            <Form.Item label="name">
                <Input defaultValue={name} onChange={handleInputChange}/>
            </Form.Item>
            <Form.Item label="sync period(ms)">
                <Input type="number" defaultValue={syncPeriod} onChange={handleSyncChange}/>
            </Form.Item>
            <Form.Item>
                <Flex align="center" justify="end" gap="middle">
                    <Button loading={updating} onClick={addAction} type="primary">add</Button>
                    <Button onClick={() => closeModal()}>cancel</Button>
                </Flex>
            </Form.Item>

        </Form>
    )
}

export default function Page() {
    const [messageApi, contextHolder] = message.useMessage();
    const [jobs, setJobs] = useState<any>([])
    const [isModalOpen, setIsModalOpen] = useState(false)

    const freshAction = () => {
        getJobs().then(res => setJobs(res))
    }


    useEffect(() => {
        let unmount = false
        getJobs().then(res => {
            if (!unmount) {
                setJobs(res)
            }
        })
        return () => {
            unmount = true
        }
    }, []);

    const panels = jobs.map((job: any) => JobPanel({
        job: job,
        onDeleted: freshAction,
        messageApi: messageApi
    }))

    const closeModal = (refresh: boolean) => {
        setIsModalOpen(false)
        if (refresh) {
            freshAction()
        }
    }

    return (<Flex>
        {contextHolder}
        <Flex gap="middle">
            {panels}
        </Flex>
        <Modal open={isModalOpen} footer={null} closeIcon={false} destroyOnClose>
            <JobModal closeModal={closeModal}/>
        </Modal>
        <FloatButton icon={<PlusOutlined/>} onClick={() => setIsModalOpen(true)}/>
    </Flex>)
}
