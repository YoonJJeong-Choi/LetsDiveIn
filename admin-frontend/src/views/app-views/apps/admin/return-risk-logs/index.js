import React, { useEffect, useState } from 'react'
import { Card, Table, Select, InputNumber, Button, Space, Tag, message } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import AdminService from 'services/AdminService'

const ReturnRiskLogsPage = () => {
	const [loading, setLoading] = useState(false)
	const [rows, setRows] = useState([])
	const [successFilter, setSuccessFilter] = useState('all')
	const [limit, setLimit] = useState(50)

	const fetchLogs = async () => {
		try {
			setLoading(true)
			const params = {
				limit
			}
			if (successFilter === 'true') params.success = true
			if (successFilter === 'false') params.success = false
			const response = await AdminService.getReturnRiskLogs(params)
			const data = response?.data ?? response
			setRows(Array.isArray(data) ? data : [])
		} catch (err) {
			console.error('반품 리스크 로그 조회 실패:', err)
			message.error(err?.response?.data?.message || '반품 리스크 로그 조회 실패')
		} finally {
			setLoading(false)
		}
	}

	useEffect(() => {
		fetchLogs()
	// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [])

	const columns = [
		{
			title: '생성시각',
			dataIndex: 'createdAt',
			key: 'createdAt',
			width: 180,
			render: (v) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-')
		},
		{
			title: 'productNo',
			dataIndex: 'productNo',
			key: 'productNo',
			width: 110
		},
		{
			title: 'optionNo',
			dataIndex: 'optionNo',
			key: 'optionNo',
			width: 110
		},
		{
			title: 'score',
			dataIndex: 'score',
			key: 'score',
			width: 90,
			render: (v) => (typeof v === 'number' ? v.toFixed(2) : '-')
		},
		{
			title: 'riskLevel',
			dataIndex: 'riskLevel',
			key: 'riskLevel',
			width: 120,
			render: (level) => {
				const colorMap = {
					LOW: 'green',
					MEDIUM: 'orange',
					HIGH: 'red',
					UNKNOWN: 'default'
				}
				return <Tag color={colorMap[level] || 'default'}>{level || 'UNKNOWN'}</Tag>
			}
		},
		{
			title: 'success',
			dataIndex: 'success',
			key: 'success',
			width: 100,
			render: (v) => (v ? <Tag color="green">true</Tag> : <Tag color="red">false</Tag>)
		},
		{
			title: 'latencyMs',
			dataIndex: 'latencyMs',
			key: 'latencyMs',
			width: 110
		},
		{
			title: 'model',
			dataIndex: 'model',
			key: 'model',
			width: 130
		},
		{
			title: 'errorMessage',
			dataIndex: 'errorMessage',
			key: 'errorMessage',
			ellipsis: true,
			render: (v) => v || '-'
		}
	]

	return (
		<Card
			title="AI 반품 리스크 로그"
			extra={
				<Space>
					<Select
						value={successFilter}
						onChange={setSuccessFilter}
						style={{ width: 130 }}
						options={[
							{ label: '전체', value: 'all' },
							{ label: '성공만', value: 'true' },
							{ label: '실패만', value: 'false' }
						]}
					/>
					<InputNumber
						min={1}
						max={200}
						value={limit}
						onChange={(v) => setLimit(Number(v) || 50)}
					/>
					<Button icon={<ReloadOutlined />} onClick={fetchLogs}>
						조회
					</Button>
				</Space>
			}
		>
			<Table
				rowKey={(record, index) => `${record.createdAt || ''}_${record.productNo || ''}_${index}`}
				loading={loading}
				columns={columns}
				dataSource={rows}
				pagination={false}
				scroll={{ x: 1200 }}
			/>
		</Card>
	)
}

export default ReturnRiskLogsPage
