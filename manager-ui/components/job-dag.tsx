'use client'
import {Graph} from '@antv/x6'

import {useEffect} from "react";
import {Snapline} from "@antv/x6-plugin-snapline";
import {parseJobGraph} from "@/utils/dag-utils";


export default function JobDag({data}: { data: any }) {
    useEffect(() => {
        const component = new Graph({
            container: document.getElementById('container') as HTMLElement,
            autoResize: true,
            background: {
                color: '#F2F7FA',
            },
        })
        component.use(new Snapline({
            enabled: true
        }))
        component.fromJSON(parseJobGraph(data))
        setTimeout(() => {
            component.centerContent()
        }, 1000)
    }, [data]);

    return (
        <div style={{width: '100%', height: '100%'}}>
            <div id="container"></div>
        </div>
    )
}