import React, { useEffect, useState } from 'react';
import { Alert, Card, Col, Row, Statistic, Table, Tag, Typography, Button, Space, message } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import AdminService from 'services/AdminService';

const { Text } = Typography;

const AiOpsOverview = () => {
	const [loading, setLoading] = useState(false);
	const [overview, setOverview] = useState(null);

	const fetchOverview = async () => {
		try {
			setLoading(true);
			const res = await AdminService.getAiOverview();
			setOverview(res?.data || res);
		} catch (err) {
			message.error(err?.response?.data?.message || 'AI 운영 지표 조회에 실패했습니다.');
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		fetchOverview();
	}, []);

	const roleColumns = [
		{ title: '역할', dataIndex: 'role', key: 'role', render: (v) => <Tag color="blue">{v}</Tag> },
		{ title: '차단 건수(오늘)', dataIndex: 'count', key: 'count' },
	];

	const accountColumns = [
		{ title: '역할', dataIndex: 'role', key: 'role', width: 120, render: (v) => <Tag>{v}</Tag> },
		{ title: '계정 ID', dataIndex: 'accountId', key: 'accountId' },
		{ title: '차단 건수(오늘)', dataIndex: 'count', key: 'count', width: 150 },
	];

	const failureColumns = [
		{ title: '시각', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
		{ title: '기능', dataIndex: 'feature', key: 'feature', width: 150, render: (v) => <Tag color="purple">{v}</Tag> },
		{ title: '역할', dataIndex: 'role', key: 'role', width: 100 },
		{ title: '계정 ID', dataIndex: 'accountId', key: 'accountId', width: 120 },
		{ title: '오류 메시지', dataIndex: 'errorMessage', key: 'errorMessage', ellipsis: true },
	];

	return (
		<Space direction="vertical" size={16} style={{ width: '100%' }}>
			<Card
				title="AI 운영 모니터링"
				extra={
					<Button icon={<ReloadOutlined />} onClick={fetchOverview} loading={loading}>
						새로고침
					</Button>
				}
			>
				<Alert
					type="info"
					showIcon
					message="OpenAI 대시보드와 함께 사용하세요"
					description="OpenAI 대시보드에서 결제/총사용량을, 이 화면에서 기능별 호출/차단/실패 로그를 확인합니다."
					className="mb-3"
				/>

				<Row gutter={[16, 16]}>
					<Col xs={24} md={12} lg={6}>
						<Card>
							<Statistic title="오늘 호출 수 (전체)" value={overview?.todayCalls?.total || 0} />
							<Text type="secondary">QnA {overview?.todayCalls?.qnaDraft || 0} / 리뷰 {overview?.todayCalls?.reviewAnalysis || 0}</Text>
						</Card>
					</Col>
					<Col xs={24} md={12} lg={6}>
						<Card>
							<Statistic title="이번달 호출 수 (전체)" value={overview?.monthCalls?.total || 0} />
							<Text type="secondary">QnA {overview?.monthCalls?.qnaDraft || 0} / 리뷰 {overview?.monthCalls?.reviewAnalysis || 0}</Text>
						</Card>
					</Col>
					<Col xs={24} md={12} lg={6}>
						<Card>
							<Statistic title="오늘 429 차단 건수" value={overview?.todayBlocked?.total || 0} />
						</Card>
					</Col>
					<Col xs={24} md={12} lg={6}>
						<Card>
							<Statistic title="이번달 429 차단 건수" value={overview?.monthBlocked?.total || 0} />
						</Card>
					</Col>
				</Row>
			</Card>

			<Row gutter={[16, 16]}>
				<Col xs={24} lg={8}>
					<Card title="일일 한도 설정값">
						<p>QnA 초안(관리자): <b>{overview?.dailyLimits?.qnaDraftAdmin ?? '-'}</b> 회</p>
						<p>QnA 초안(파트너): <b>{overview?.dailyLimits?.qnaDraftPartner ?? '-'}</b> 회</p>
						<p>리뷰 분석(파트너): <b>{overview?.dailyLimits?.reviewAnalysisPartner ?? '-'}</b> 회</p>
					</Card>
				</Col>
				<Col xs={24} lg={16}>
					<Card title="역할별 오늘 차단 건수">
						<Table
							rowKey={(row) => `${row.role}`}
							columns={roleColumns}
							dataSource={overview?.blockedByRoleToday || []}
							pagination={false}
							loading={loading}
							size="small"
						/>
					</Card>
				</Col>
			</Row>

			<Card title="계정별 오늘 차단 건수 (Top 10)">
				<Table
					rowKey={(row) => `${row.role}-${row.accountId}`}
					columns={accountColumns}
					dataSource={overview?.blockedByAccountToday || []}
					pagination={false}
					loading={loading}
					size="small"
				/>
			</Card>

			<Card title="최근 실패 로그 (최신 20건)">
				<Table
					rowKey={(row, idx) => `${row.feature}-${row.accountId}-${idx}`}
					columns={failureColumns}
					dataSource={overview?.recentFailures || []}
					pagination={false}
					loading={loading}
					size="small"
				/>
			</Card>
		</Space>
	);
};

export default AiOpsOverview;
