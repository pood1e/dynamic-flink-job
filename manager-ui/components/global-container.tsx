'use client'
import {Breadcrumb, ConfigProvider, Flex, Layout, theme, ThemeConfig, Typography} from "antd";
import Logo from '@/public/logo.svg'
import Icon, {HomeOutlined} from "@ant-design/icons";
import SiderMenu from "@/components/sider-menu";
import {useSelectedLayoutSegments} from "next/navigation";
import React from "react";


function DefaultLayout({children}: React.PropsWithChildren) {
    const token = theme.useToken();
    const curPaths = useSelectedLayoutSegments()
    const breadcrumbItems = curPaths.length > 0 ? [{
        href: '/',
        title: <HomeOutlined/>
    }] : []
    let curPath = ""
    curPaths.forEach((cur: any, index) => {
        curPath = curPath + "/" + cur
        const routeItem: any = {
            title: cur
        }
        if (index !== curPaths.length - 1) {
            routeItem['href'] = curPath
        }
        breadcrumbItems.push(routeItem)
    })

    return (
        <Layout style={{height: '100%'}}>
            <Layout.Sider style={{borderRight: '1px solid ' + token.token.colorSplit}}>
                <Flex style={{height: token.token.Layout?.headerHeight}}
                      align='center' justify='center' gap={"small"}>
                    <Icon style={{fontSize: token.token.fontSizeHeading1}} component={Logo}/>
                    <Typography.Title level={3}>DJ</Typography.Title>
                </Flex>
                <SiderMenu/>
            </Layout.Sider>
            <Layout>
                <Layout.Header style={{borderBottom: '1px solid ' + token.token.colorSplit}}>
                    <Flex style={{height: '100%'}} align='center'>
                        <Breadcrumb items={breadcrumbItems}/>
                    </Flex>
                </Layout.Header>
                <Layout.Content style={{padding: '24px'}}>
                    {children}
                </Layout.Content>
            </Layout>
        </Layout>
    )
}

export default function GlobalContainer({children}: React.PropsWithChildren) {
    const token = theme.useToken();
    const globalTheme: ThemeConfig = {
        cssVar: true,
        hashed: false,
        components: {
            Layout: {
                headerBg: token.token.colorBgLayout,
                siderBg: token.token.colorBgLayout,
                headerHeight: token.token.controlHeight * 2,
                headerPadding: `0 ${token.token.controlHeightLG * 1.25}px`,
            },
            Menu: {
                itemBg: token.token.colorBgLayout
            }
        }
    };

    return (
        <ConfigProvider theme={globalTheme}>
            <DefaultLayout>{children}</DefaultLayout>
        </ConfigProvider>
    );
}
