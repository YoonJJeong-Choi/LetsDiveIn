import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Modal, Input, Form, message, Tag, Space, Row, Col, Alert, Statistic, Descriptions, Spin, Tabs, Badge } from 'antd';
import { CheckCircleOutlined, CloseCircleOutlined, ReloadOutlined, InfoCircleOutlined, ShoppingOutlined } from '@ant-design/icons';
import AdminService from 'services/AdminService';

const { TextArea } = Input;

const ProductApproval = () => {
	const [allProducts, setAllProducts] = useState([]); // 구형: 전체 목록(임시 유지)
	const [products, setProducts] = useState([]); // 테이블 표시용 (서버 페이지네이션 items)
	const [loading, setLoading] = useState(false);
	const [statusFilter, setStatusFilter] = useState(null); // 기본값: 전체
	const [approvalSubFilter, setApprovalSubFilter] = useState('PENDING'); // 승인 대기 탭 내부 필터: 'PENDING' 또는 'PENDING_UPDATE'
	const [rejectModalVisible, setRejectModalVisible] = useState(false);
	const [selectedProduct, setSelectedProduct] = useState(null);
	const [rejectForm] = Form.useForm();
	// 서버 페이지네이션 상태
	const [currentPage, setCurrentPage] = useState(1);
	const [pageSize, setPageSize] = useState(10);
	const [totalItems, setTotalItems] = useState(0);
	// 상태별 카운트(요약 API)
	const [statusCounts, setStatusCounts] = useState({ PENDING: 0, PENDING_UPDATE: 0, ACTIVE: 0, REJECTED: 0, INACTIVE: 0 });

	// 상품 목록 조회
	const fetchProducts = async () => {
		try {
			setLoading(true);
			// 승인 대기 탭인 경우 서브필터 적용
			let filterToUse = statusFilter;
			if (statusFilter === 'PENDING_APPROVAL') {
				filterToUse = approvalSubFilter;
			}
			console.log('[Admin Products] request params =>', { status: filterToUse, page: currentPage, size: pageSize });
			const response = await AdminService.getAllProducts(filterToUse, currentPage, pageSize);
			const data = response?.data || response;
			const meta = data?.meta;
			const rawItems = Array.isArray(data) ? data : (data?.products || data?.items || data?.data || []); // 배열/래핑/표준 호환
			console.log('[Admin Products] response meta =>', { page: meta?.page ?? data?.page, size: meta?.size ?? data?.size, total: meta?.total ?? data?.total ?? data?.totalCount, itemsCount: Array.isArray(rawItems) ? rawItems.length : 0 });

			// 상태 분포 진단 로그
			const normStatus = (p) => ((p?.productActiveStatus ?? p?.status ?? p?.productStatus ?? '') + '').trim().toUpperCase();
			const statusCounts = (Array.isArray(rawItems) ? rawItems : []).reduce((acc, p) => {
				const s = normStatus(p) || 'UNKNOWN';
				acc[s] = (acc[s] || 0) + 1;
				return acc;
			}, {});
			console.log('[Admin Products] statusCounts (raw):', statusCounts);

			// 로컬 필터 백업(서버 필터 미지원 대비)
			let items = Array.isArray(rawItems) ? rawItems : [];
			const uniqueStatuses = new Set(items.map(p => normStatus(p)));

			if (statusFilter === 'PENDING_APPROVAL') {
				// 승인 대기 합산 탭: 등록/수정 모두 포함
				// 서버가 이미 합산 처리해주면 그대로 사용, 아니면 로컬 필터 적용
				const onlyPendingKinds = [...uniqueStatuses].every(s => s === 'PENDING' || s === 'PENDING_UPDATE');
				if (!onlyPendingKinds) {
					items = items.filter(p => {
						const s = normStatus(p);
						return s === 'PENDING' || s === 'PENDING_UPDATE';
					});
				}
			} else if (filterToUse) {
				// 서버가 이미 상태 필터를 적용했는지 판별: 모든 행이 동일 상태이면 서버 필터로 간주
				const wanted = (filterToUse + '').trim().toUpperCase();
				const serverSeemsFiltered = uniqueStatuses.size === 1 && uniqueStatuses.has(wanted);
				if (!serverSeemsFiltered) {
					items = items.filter(p => normStatus(p) === wanted);
				}
			}

			// 재시도 백업 제거: 서버 응답 기준으로만 표시

			const total = (meta?.total ?? data?.total ?? data?.totalCount ?? items.length);
			setProducts(items);
			setTotalItems(Number(total) || 0);

			// 탭 카운트 계산을 위해 전체 목록(상태 무필터)도 1페이지로만 가져와 총합 계산
			const allResp = await AdminService.getAllProducts(null, 1, 1000);
			const allData = allResp?.data || allResp;
			const allItems = Array.isArray(allData) ? allData : (allData?.products || allData?.items || allData?.data || []);
			setAllProducts(Array.isArray(allItems) ? allItems : []);
		} catch (error) {
			const errorMessage = error.response?.data?.message || error.message || '상품 목록을 불러오는데 실패했습니다.';
			message.error(errorMessage);
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		// 상태/서브필터 변경 시 1페이지로 리셋
		setCurrentPage(1);
	}, [statusFilter, approvalSubFilter]);

	useEffect(() => {
		fetchProducts();
		fetchStatusCounts();
	// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [currentPage, pageSize, statusFilter, approvalSubFilter]);

	const fetchStatusCounts = async () => {
		try {
			const resp = await AdminService.getAdminProductStatusCounts();
			const data = resp?.data ?? resp ?? {};
			setStatusCounts({
				PENDING: Number(data.PENDING || 0),
				PENDING_UPDATE: Number(data.PENDING_UPDATE || 0),
				ACTIVE: Number(data.ACTIVE || 0),
				REJECTED: Number(data.REJECTED || 0),
				INACTIVE: Number(data.INACTIVE || 0),
			});
		} catch (e) {
			// fallback 유지
		}
	};
	// 상품 승인
	const handleApprove = (product) => {
		Modal.confirm({
			title: '상품 승인',
			content: (
				<div>
					<p>다음 상품을 승인하시겠습니까?</p>
					<Descriptions column={1} size="small" bordered>
						<Descriptions.Item label="상품명">{product.productName}</Descriptions.Item>
						{product.partnerName && (
							<Descriptions.Item label="파트너명">
								<strong>{product.partnerName}</strong>
							</Descriptions.Item>
						)}
						<Descriptions.Item label="카테고리">
							{product.productType}
							{product.productSubType && ` / ${product.productSubType}`}
						</Descriptions.Item>
						<Descriptions.Item label="가격">
							{product.minPrice?.toLocaleString()}원
							{product.maxPrice && product.maxPrice !== product.minPrice && (
								<span> ~ {product.maxPrice.toLocaleString()}원</span>
							)}
						</Descriptions.Item>
					</Descriptions>
					<p style={{ marginTop: '16px', color: '#52c41a' }}>
						승인 시 상품이 고객에게 노출됩니다.
					</p>
				</div>
			),
			okText: '승인',
			okType: 'primary',
			cancelText: '취소',
			onOk: async () => {
				try {
					const response = await AdminService.approveProduct(product.productNo);
					if (response && response.success) {
						message.success('상품이 승인되었습니다.');
						fetchProducts();
					} else {
						message.error(response?.message || '상품 승인에 실패했습니다.');
					}
				} catch (error) {
					const errorMessage = error.response?.data?.message || error.message || '상품 승인에 실패했습니다.';
					message.error(errorMessage);
					// Error handled by message.error above
				}
			}
		});
	};

	// 상품 거절 모달 열기
	const handleReject = (product) => {
		setSelectedProduct(product);
		setRejectModalVisible(true);
		rejectForm.resetFields();
	};

	// 상품 거절
	const handleRejectSubmit = async (values) => {
		if (!values.rejectionReason || !values.rejectionReason.trim()) {
			message.error('거절 사유를 입력해주세요.');
			return;
		}

		try {
			const response = await AdminService.rejectProduct(
				selectedProduct.productNo,
				values.rejectionReason.trim()
			);
			if (response && response.success) {
				message.success('상품이 거절되었습니다.');
				setRejectModalVisible(false);
				setSelectedProduct(null);
				rejectForm.resetFields();
				fetchProducts();
			} else {
				message.error(response?.message || '상품 거절에 실패했습니다.');
			}
		} catch (error) {
			const errorMessage = error.response?.data?.message || error.message || '상품 거절에 실패했습니다.';
			message.error(errorMessage);
			// Error handled by message.error above
		}
	};

	// 상태 배지 색상
	const getStatusTag = (status) => {
		const tags = {
			PENDING: { color: 'orange', text: '승인 대기' },
			ACTIVE: { color: 'green', text: '활성' },
			REJECTED: { color: 'red', text: '거절' },
			INACTIVE: { color: 'default', text: '비활성' },
			PENDING_UPDATE: { color: 'blue', text: '수정 승인 대기' },
		};
		const tag = tags[status] || { color: 'default', text: status };
		return <Tag color={tag.color}>{tag.text}</Tag>;
	};

	// 승인 대기 중인 상품 수 계산 (전체 목록 기준)
	const pendingRegistrationCount = statusCounts.PENDING;
	const pendingUpdateCount = statusCounts.PENDING_UPDATE;
	const pendingTotalCount = pendingRegistrationCount + pendingUpdateCount;

	// 상태 컬럼: 전체 탭일 때만 표시 (탭으로 필터링된 경우는 중복)
	const showStatusColumn = statusFilter === null || statusFilter === 'ALL';
	
	// 작업 컬럼: 승인 대기 중 탭일 때만 표시 (PENDING, PENDING_UPDATE 또는 PENDING_APPROVAL 상태)
	const showActionColumn = statusFilter === 'PENDING' || statusFilter === 'PENDING_UPDATE' || statusFilter === 'PENDING_APPROVAL';

	const tableColumns = [
		{
			title: '상품 번호',
			dataIndex: 'productNo',
			key: 'productNo',
			width: 100,
		},
		{
			title: '상품명',
			dataIndex: 'productName',
			key: 'productName',
			render: (text) => (
				<div>
					<ShoppingOutlined style={{ marginRight: 8 }} />
					<strong>{text}</strong>
				</div>
			),
		},
		{
			title: '카테고리',
			key: 'category',
			render: (_, record) => (
				<div>
					{record.productType}
					{record.productSubType && (
						<span className="text-muted"> / {record.productSubType}</span>
					)}
				</div>
			),
		},
		{
			title: '가격',
			key: 'price',
			width: 150,
			render: (_, record) => (
				<div>
					{record.minPrice?.toLocaleString()}원
					{record.maxPrice && record.maxPrice !== record.minPrice && (
						<div className="text-muted" style={{ fontSize: '12px' }}>
							~ {record.maxPrice.toLocaleString()}원
						</div>
					)}
				</div>
			),
		},
		// 상태 컬럼: 전체 탭일 때만 표시
		...(showStatusColumn ? [{
			title: '상태',
			dataIndex: 'productActiveStatus',
			key: 'productActiveStatus',
			width: 100,
			render: (status) => getStatusTag(status),
		}] : []),
		{
			title: '신청일',
			dataIndex: 'productCreatedAt',
			key: 'productCreatedAt',
			width: 120,
			render: (date) => new Date(date).toLocaleDateString(),
		},
		// 작업 컬럼: 승인 대기 중 탭일 때만 표시
		...(showActionColumn ? [{
			title: '작업',
			key: 'action',
			width: 150,
			render: (_, record) => (
				<Space>
					<Button
						type="primary"
						icon={<CheckCircleOutlined />}
						onClick={() => handleApprove(record)}
						size="small"
					>
						승인
					</Button>
					<Button
						danger
						icon={<CloseCircleOutlined />}
						onClick={() => handleReject(record)}
						size="small"
					>
						거절
					</Button>
				</Space>
			),
		}] : []),
	];

	const tabItems = [
		{
			key: 'ALL',
			label: '전체',
		},
		{
			key: 'PENDING_APPROVAL',
			label: (
				<span>
					승인 대기 ({pendingTotalCount})
				</span>
			),
			children: (
				<Tabs
					type="card"
					activeKey={approvalSubFilter}
					items={[
						{
							key: 'PENDING',
							label: `등록 (${pendingRegistrationCount})`,
						},
						{
							key: 'PENDING_UPDATE',
							label: `수정 (${pendingUpdateCount})`,
						},
					]}
					onChange={(key) => setApprovalSubFilter(key)}
					style={{ marginBottom: 16 }}
				/>
			),
		},
		{
			key: 'ACTIVE',
			label: `활성 (${statusCounts.ACTIVE})`,
		},
		{
			key: 'REJECTED',
			label: `거절 (${statusCounts.REJECTED})`,
		},
		{
			key: 'INACTIVE',
			label: `비활성 (${statusCounts.INACTIVE})`,
		},
	];

	const handleTabChange = (key) => {
		if (key === 'ALL') {
			setStatusFilter(null);
		} else if (key === 'PENDING_APPROVAL') {
			setStatusFilter('PENDING_APPROVAL');
			setApprovalSubFilter('PENDING'); // 기본값으로 등록 탭 선택
		} else {
			setStatusFilter(key);
		}
	};

	// 전체 탭일 때 activeKey 설정
	const activeTabKey = statusFilter === null ? 'ALL' : statusFilter;

	return (
		<>
			<Card>
				<Row gutter={16} style={{ marginBottom: 24 }}>
					<Col span={8}>
						<Statistic
							title="등록 승인 대기"
							value={pendingRegistrationCount}
							prefix={<InfoCircleOutlined />}
							valueStyle={{ color: '#fa8c16' }}
						/>
					</Col>
					<Col span={8}>
						<Statistic
							title="수정 승인 대기"
							value={pendingUpdateCount}
							prefix={<InfoCircleOutlined />}
							valueStyle={{ color: '#1890ff' }}
						/>
					</Col>
					<Col span={8}>
						<Statistic
							title="총 승인 대기"
							value={pendingTotalCount}
							prefix={<InfoCircleOutlined />}
							valueStyle={{ color: '#fa8c16', fontWeight: 'bold' }}
						/>
					</Col>
				</Row>

				<Space style={{ marginBottom: 16 }}>
					<Button
						icon={<ReloadOutlined />}
						onClick={fetchProducts}
						loading={loading}
					>
						새로고침
					</Button>
				</Space>

				<Tabs
					activeKey={activeTabKey}
					items={tabItems}
					onChange={handleTabChange}
					style={{ marginBottom: 16 }}
				/>

				<Spin spinning={loading}>
					<Table
						columns={tableColumns}
						dataSource={products}
						rowKey="productNo"
						pagination={{
							current: currentPage,
							pageSize: pageSize,
							total: totalItems,
							showSizeChanger: true,
							showTotal: (total) => `총 ${total}개 상품`,
							onChange: (p, s) => {
								setCurrentPage(p);
								setPageSize(s);
							},
						}}
						locale={{
							emptyText: statusFilter === 'PENDING' || statusFilter === 'PENDING_UPDATE'
								? '승인 대기 중인 상품이 없습니다.'
								: '상품이 없습니다.',
						}}
						expandable={{
							expandedRowRender: (record) => (
								<div style={{ padding: '16px', background: '#fafafa' }}>
									<Descriptions title="상품 상세 정보" column={2} bordered size="small">
										<Descriptions.Item label="상품 번호" span={1}>
											{record.productNo}
										</Descriptions.Item>
										<Descriptions.Item label="상태" span={1}>
											{getStatusTag(record.productActiveStatus)}
										</Descriptions.Item>
										<Descriptions.Item label="상품명" span={2}>
											{record.productName}
										</Descriptions.Item>
										{/* 파트너 정보 */}
										{record.partnerName && (
											<>
												<Descriptions.Item label="파트너명" span={1}>
													<strong>{record.partnerName}</strong>
												</Descriptions.Item>
												<Descriptions.Item label="연락처" span={1}>
													{record.partnerContact || '-'}
												</Descriptions.Item>
												{record.partnerEmail && (
													<Descriptions.Item label="이메일" span={2}>
														{record.partnerEmail}
													</Descriptions.Item>
												)}
											</>
										)}
										<Descriptions.Item label="카테고리" span={1}>
											{record.productType}
											{record.productSubType && ` / ${record.productSubType}`}
										</Descriptions.Item>
										<Descriptions.Item label="가격" span={1}>
											{record.minPrice?.toLocaleString()}원
											{record.maxPrice && record.maxPrice !== record.minPrice && (
												<span> ~ {record.maxPrice.toLocaleString()}원</span>
											)}
										</Descriptions.Item>
										<Descriptions.Item label="상품 설명" span={2}>
											{record.productDescription}
										</Descriptions.Item>
										<Descriptions.Item label="신청일" span={1}>
											{new Date(record.productCreatedAt).toLocaleString()}
										</Descriptions.Item>
										<Descriptions.Item label="옵션 수" span={1}>
											{record.options?.length || 0}개
										</Descriptions.Item>
										{record.rejectionReason && (
											<Descriptions.Item label="거절 사유" span={2}>
												<span style={{ color: '#ff4d4f' }}>{record.rejectionReason}</span>
											</Descriptions.Item>
										)}
									</Descriptions>
									{record.options && record.options.length > 0 && (
										<div style={{ marginTop: 16 }}>
											<h4 style={{ marginBottom: 8 }}>옵션 목록</h4>
											<Table
												columns={[
													{ title: '옵션 번호', dataIndex: 'optionNo', key: 'optionNo', width: 100 },
													{ title: '색상', dataIndex: 'color', key: 'color', render: (text) => text || '-' },
													{ title: '사이즈', dataIndex: 'size', key: 'size', render: (text) => text || '-' },
													{
														title: '추가 가격',
														dataIndex: 'addPrice',
														key: 'addPrice',
														render: (price) => price ? `${price.toLocaleString()}원` : '0원',
													},
													{
														title: '총 가격',
														dataIndex: 'totalPrice',
														key: 'totalPrice',
														render: (price) => `${price.toLocaleString()}원`,
													},
													{
														title: '상태',
														dataIndex: 'optionStatus',
														key: 'optionStatus',
														width: 120,
														render: (status) => {
															if (!status) return '-';
															const statusMap = {
																PENDING: { color: 'orange', text: '승인 대기' },
																ACTIVE: { color: 'green', text: '활성' },
																REJECTED: { color: 'red', text: '거절' },
																INACTIVE: { color: 'default', text: '비활성' },
																PENDING_UPDATE: { color: 'blue', text: '수정 승인 대기' },
															};
															const statusInfo = statusMap[status] || { color: 'default', text: status };
															return <Tag color={statusInfo.color}>{statusInfo.text}</Tag>;
														},
													},
												]}
												dataSource={record.options}
												rowKey="optionNo"
												pagination={false}
												size="small"
											/>
										</div>
									)}
								</div>
							),
						}}
					/>
				</Spin>
			</Card>

			{/* 거절 모달 */}
			<Modal
				title="상품 거절"
				open={rejectModalVisible}
				onCancel={() => {
					setRejectModalVisible(false);
					setSelectedProduct(null);
					rejectForm.resetFields();
				}}
				onOk={() => rejectForm.submit()}
				okText="거절"
				okButtonProps={{ danger: true }}
				cancelText="취소"
			>
				{selectedProduct && (
					<div style={{ marginBottom: 16 }}>
						<p>다음 상품을 거절하시겠습니까?</p>
						<Descriptions column={1} size="small" bordered>
							<Descriptions.Item label="상품명">{selectedProduct.productName}</Descriptions.Item>
							<Descriptions.Item label="카테고리">
								{selectedProduct.productType}
								{selectedProduct.productSubType && ` / ${selectedProduct.productSubType}`}
							</Descriptions.Item>
							<Descriptions.Item label="가격">
								{selectedProduct.minPrice?.toLocaleString()}원
								{selectedProduct.maxPrice && selectedProduct.maxPrice !== selectedProduct.minPrice && (
									<span> ~ {selectedProduct.maxPrice.toLocaleString()}원</span>
								)}
							</Descriptions.Item>
						</Descriptions>
					</div>
				)}
				<Form
					form={rejectForm}
					onFinish={handleRejectSubmit}
					layout="vertical"
				>
					<Form.Item
						name="rejectionReason"
						label="거절 사유"
						rules={[
							{ required: true, message: '거절 사유를 입력해주세요.' },
							{ min: 10, message: '거절 사유는 최소 10자 이상 입력해주세요.' }
						]}
					>
						<TextArea
							rows={4}
							placeholder="거절 사유를 입력해주세요. (최소 10자 이상)"
							maxLength={500}
							showCount
						/>
					</Form.Item>
				</Form>
			</Modal>
		</>
	);
};

export default ProductApproval;
