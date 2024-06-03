'use client'

import {
    Button,
    Card,
    Divider,
    Flex,
    FloatButton,
    Form,
    Input,
    InputRef,
    message,
    Modal,
    Space,
    Tag,
    Typography,
    Upload,
    UploadProps
} from "antd";
import Icon, {DeleteOutlined, InboxOutlined, PlusOutlined} from "@ant-design/icons";
import React, {useEffect, useRef, useState} from "react";
import JarSvg from "@/public/jar.svg"
import {useImmer} from "use-immer";
import {addPlugin, deletePlugin, getPlugin, getPlugins, updatePlugin} from "@/client/client-api";

function UploadPanel({pluginId}: { pluginId: any }) {
    const [messageApi, contextHolder] = message.useMessage();
    const props: UploadProps = {
        name: 'file',
        action: `/api/plugin/${pluginId}/jar`,
        onChange: (info) => {
            const {status} = info.file;
            if (status === 'done') {
                messageApi.success(`${info.file.name} file uploaded successfully.`);
            } else if (status === 'error') {
                messageApi.error(`${info.file.name} file upload failed.`);
            }
        },
        showUploadList: false
    }

    return (
        <div>
            {contextHolder}
            <Upload.Dragger {...props} style={{height: 0}}>
                <p className="ant-upload-drag-icon">
                    <InboxOutlined/>
                </p>
                <p className="ant-upload-text">Click or drag file to this area to upload</p>
                <p className="ant-upload-hint">
                    Support for a single or bulk upload. Strictly prohibited from uploading company data or other
                    banned files.
                </p>
            </Upload.Dragger>
        </div>
    )
}

function PackageTag({pkg, onClose}: { pkg: string, onClose: Function }) {
    return (
        <Tag closeIcon onClose={() => onClose(pkg)}> {pkg}</Tag>
    )
}

function AddPackageTag({onAdded}: { onAdded: Function }) {
    const [showInput, setShowInput] = useState(false)
    const [inputValue, setInputValue] = useState('')
    const ref = useRef<InputRef>(null)
    useEffect(() => {
        if (showInput) {
            ref.current?.focus()
        }
    }, [showInput]);

    const commit = () => {
        if (inputValue) {
            onAdded(inputValue)
        }
        setShowInput(false)
    }

    const edit = () => {
        setInputValue('')
        setShowInput(true)
    }

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setInputValue(e.target.value);
    }

    if (showInput) {
        return (
            <Input size="small" type="text" onBlur={commit} onPressEnter={commit} ref={ref}
                   onChange={handleInputChange}/>
        )
    }
    return <Tag icon={<PlusOutlined/>} onClick={edit}/>
}


function PluginSettingPanel({plugin, closeModal}: {
    plugin: any,
    closeModal: Function
}) {
    const [name, setName] = useState(plugin.name ? plugin.name : '')
    const [pkgArray, updatePkgArray] = useImmer(plugin.packages ? plugin.packages : [])
    const [updating, setUpdating] = useState(false);
    const removePkg = (pkg: string) => {
        updatePkgArray((draft: any) => {
            draft.splice(pkgs.indexOf(pkg), 1)
        })
    }

    const addPkg = (pkg: string) => {
        updatePkgArray((draft: any) => {
            draft.push(pkg)
        })
    }

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setName(e.target.value);
    }

    const pkgs = pkgArray.map((pkg: string) => <PackageTag key={pkg} pkg={pkg} onClose={removePkg}/>)

    const updateAction = () => {
        const obj: any = {
            ...plugin
        }
        if (name !== plugin.name) {
            obj['name'] = name
        }
        if (pkgArray !== plugin.packages) {
            obj['packages'] = pkgArray
        }
        setUpdating(true)
        const afterChange = (res: any) => {
            setUpdating(false)
            closeModal(true)
        }
        if (plugin.id) {
            updatePlugin(obj).then(afterChange)
        } else {
            addPlugin(obj).then(afterChange)
        }

    }

    return (
        <Form labelCol={{span: 4}}>
            <Form.Item label="name">
                <Input defaultValue={name} onChange={handleInputChange}/>
            </Form.Item>
            <Form.Item label="packages">
                <Flex gap="4px 4px" wrap>
                    {pkgs}
                    <Space.Compact>
                        <AddPackageTag onAdded={addPkg}/>
                    </Space.Compact>
                </Flex>
            </Form.Item>
            <Form.Item>
                <Flex align="center" justify="end" gap="middle">
                    <Button loading={updating} onClick={updateAction} type="primary">update</Button>
                    <Button onClick={() => closeModal()}>cancel</Button>
                </Flex>
            </Form.Item>
            {plugin.id && <>
                <Divider>JAR</Divider>
                <UploadPanel pluginId={plugin.id}/>
            </>}
        </Form>
    )
}


function PluginModal({pluginId, closeModal}: {
    pluginId: string | null,
    closeModal: Function
}) {
    const [plugin, setPlugin] = useState<any | null>()

    useEffect(() => {
        let unmount = false
        if (!pluginId) {
            setPlugin({})
        } else {
            getPlugin(pluginId).then(res => {
                if (!unmount) {
                    setPlugin(res)
                }
            })
        }
        return () => {
            unmount = true
        }
    }, [pluginId]);


    return (
        <>
            {plugin && <PluginSettingPanel plugin={plugin} closeModal={closeModal}/>}
        </>
    )
}

function PluginPanel({plugin, messageApi, onDeleted, onClick}: {
    plugin: any,
    messageApi: any,
    onDeleted: Function,
    onClick: Function
}) {
    const deleteAction = () => {
        deletePlugin(plugin.id).then(res => {
            if (res === 200) {
                onDeleted()
                messageApi.success("delete successfully.");
            } else {
                messageApi.error("delete failed")
            }
        })
    }
    const deleteIcon = <Button icon={<DeleteOutlined/>} onClick={event => {
        event.stopPropagation()
        deleteAction()
    }} danger/>
    const titleNode = (
        <Flex align="center" gap="small">
            <Icon component={JarSvg} style={{fontSize: '1.5rem'}}/>
            <span>{plugin.name}</span>
        </Flex>
    )

    return (
        <Card key={plugin.hash} title={titleNode} extra={deleteIcon} style={{minWidth: '200px'}}
              onClick={() => onClick(plugin.id)}
              hoverable>
            {plugin.pluginHash ?
                <Typography.Paragraph type="secondary">hash: {plugin.pluginHash}</Typography.Paragraph> : null}
            {plugin.classes ? <Typography.Paragraph>classes: {plugin.classes.length}</Typography.Paragraph> : null}
            {plugin.packages ? <Typography.Paragraph>packages: {plugin.packages.length}</Typography.Paragraph> : null}
        </Card>
    )
}

function PluginsPanel() {
    const [plugins, setPlugins] = useState<Array<any>>([]);
    const [curPlugin, setCurPlugin] = useState<string | null | undefined>(undefined);
    const [messageApi, contextHolder] = message.useMessage();
    const freshPlugins = () => {
        getPlugins().then(jars => setPlugins(jars))
    }
    useEffect(() => {
        let unmount = false
        getPlugins().then(plugins => {
            if (!unmount) {
                setPlugins(plugins)
            }
        })
        return () => {
            unmount = true
        }
    }, [])

    const closeModal = (refresh: boolean) => {
        setCurPlugin(undefined)
        if (refresh) {
            freshPlugins()
        }
    }

    const panel = plugins.map(plugin => <PluginPanel key={plugin.id} plugin={plugin} messageApi={messageApi}
                                                     onDeleted={freshPlugins}
                                                     onClick={(pluginId: any) => setCurPlugin(pluginId)}/>)
    return (
        <Flex gap='middle'>
            {contextHolder}
            {panel}
            <Modal title={curPlugin} open={curPlugin !== undefined} footer={null}
                   closeIcon={false} destroyOnClose>
                {curPlugin !== undefined && <PluginModal pluginId={curPlugin} closeModal={closeModal}/>}
            </Modal>
            <FloatButton icon={<PlusOutlined/>} onClick={() => setCurPlugin(null)}/>
        </Flex>
    )
}

export default function Page() {
    return (
        <PluginsPanel/>
    )
}
