'use client'
import PluginSvg from '@/public/bx-extension.svg'
import TaskSvg from '@/public/task.svg'
import Icon from "@ant-design/icons"
import {useRouter, useSelectedLayoutSegment} from "next/navigation";
import {Menu, MenuProps} from "antd";

const menu: any = [
    {
        key: 'job',
        label: 'Job',
        icon: <Icon component={TaskSvg}/>
    },
    {
        key: 'plugin',
        label: 'Plugin',
        icon: <Icon component={PluginSvg}/>
    }
]

export default function SiderMenu() {
    const router = useRouter()
    const segment = useSelectedLayoutSegment()

    const itemMap: any = Object.fromEntries(menu.map((item: any) => [item.key, item]))
    const items: any = Object.values(itemMap)
    const defaultSelected = segment ? [itemMap[segment].key] : []

    const onClick: MenuProps['onClick'] = (e) => {
        router.push("/" + e.key)
    };

    return (
        <Menu onClick={onClick} defaultSelectedKeys={defaultSelected} items={items}></Menu>
    )
}