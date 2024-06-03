'use client'

export async function getPluginClasses() {
    const response = await fetch('/api/plugin/classes')
    return await response.json()
}

export async function updatePlugin(plugin: any) {
    return await fetch('/api/plugin/' + plugin.id, {
        method: 'PUT',
        body: JSON.stringify(plugin),
        headers: {
            "Content-Type": "application/json"
        }
    })
}

export async function updateFunction(func: any) {
    return await fetch('/api/function/' + func.id, {
        method: 'PUT',
        body: JSON.stringify(func),
        headers: {
            "Content-Type": "application/json"
        }
    })
}


export async function addPlugin(plugin: any) {
    return await fetch('/api/plugin', {
        method: 'POST',
        body: JSON.stringify(plugin),
        headers: {
            "Content-Type": "application/json"
        }
    })
}

export async function deletePlugin(hash: string) {
    const response = await fetch('/api/plugin/' + hash, {
        method: 'DELETE'
    })
    return response.status
}

export async function deleteFunction(functionId: string) {
    const response = await fetch('/api/function/' + functionId, {
        method: 'DELETE'
    })
    return response.status
}

export async function getJobs() {
    const response = await fetch('/api/job')
    return await response.json()
}

export async function getJob(jobId: string) {
    const response = await fetch(`/api/job/${jobId}`)
    return await response.json()
}

export async function addJob(job: any) {
    return await fetch('/api/job', {
        method: 'POST',
        body: JSON.stringify(job),
        headers: {
            "Content-Type": "application/json"
        }
    })
}

export async function updateJob(job: any) {
    return await fetch('/api/job/' + job.id, {
        method: 'PUT',
        body: JSON.stringify(job),
        headers: {
            "Content-Type": "application/json"
        }
    })
}

export async function getFunction(functionId: string) {
    const response = await fetch(`/api/function/${functionId}`)
    return await response.json()
}

export async function addFunction(func: any) {
    const response = await fetch('/api/function', {
        method: 'POST',
        body: JSON.stringify(func),
        headers: {
            "Content-Type": "application/json"
        }
    })
    return await response.json()
}

export async function getJobGraph(jobId: string) {
    const jobData = await getJob(jobId)
    if (!jobData.functions) {
        return jobData
    }
    jobData.functions = await Promise.all(jobData.functions.map((fun: string) => {
        return getFunction(fun)
    }))
    return jobData
}

export async function getPlugins() {
    const response = await fetch('/api/plugin')
    return await response.json()
}

export async function getPlugin(pluginId: string) {
    const response = await fetch('/api/plugin/' + pluginId)
    return await response.json()
}

export async function deleteJob(job: string) {
    const response = await fetch('/api/job/' + job, {
        method: 'DELETE'
    })
    return response.status
}