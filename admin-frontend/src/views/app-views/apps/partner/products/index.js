import React, { useEffect, useMemo, useState } from 'react';
import { Alert, Badge, Button, Card, Col, Input, Menu, Modal, Row, Select, Spin, Table, Tabs, Tag, Tooltip, message } from 'antd';
import { DeleteOutlined, EditOutlined, PlusCircleOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import AvatarStatus from 'components/shared-components/AvatarStatus';
import EllipsisDropdown from 'components/shared-components/EllipsisDropdown';
import Flex from 'components/shared-components/Flex';
import { cancelProductUpdate, deleteProduct, fetchMyProducts } from 'store/slices/partnerSlice';
import PartnerService from 'services/PartnerService';
import utils from 'utils';

const { Option } = Select;

const PRODUCT_TYPE_LABELS = {
	SWIMSUIT_MEN: '남성 수영복',
	SWIMSUIT_WOMEN: '여성 수영복',
	SWIMSUIT_KIDS: '아동 수영복',
	SWIM_CAP: '수모',
	SWIM_GOGGLES: '수경',
	FINS: '오리발',
	SWIM_TOY: '수영용품',
	ETC: '기타',
};

const STATUS_META = {
	PENDING: { label: '승인 대기', badge: 'processing', color: 'blue' },
	ACTIVE: { label: '활성', badge: 'success', color: 'green' },
	REJECTED: { label: '거절', badge: 'error', color: 'red' },
	INACTIVE: { label: '비활성', badge: 'default', color: 'default' },
	PENDING_UPDATE: { label: '수정 승인 대기', badge: 'warning', color: 'orange' },
};

const formatPrice = (price) => `${Number(price || 0).toLocaleString()}원`;

const PartnerProducts = () => {
	const dispatch = useDispatch();
	const navigate = useNavigate();
	const { products, loading, error } = useSelector((state) => state.partner);
	const [searchText, setSearchText] = useState('');
	const [statusFilter, setStatusFilter] = useState(null);
	const [approvalSubFilter, setApprovalSubFilter] = useState('PENDING');
	const [typeFilter, setTypeFilter] = useState('ALL');
	const [statusCounts, setStatusCounts] = useState({ PENDING: 0, PENDING_UPDATE: 0, ACTIVE: 0, REJECTED: 0, INACTIVE: 0 });

	useEffect(() => {
		dispatch(fetchMyProducts());
	}, [dispatch]);

	useEffect(() => {
		const loadCounts = async () => {
			try {
				const resp = await PartnerService.getMyProductStatusCounts();
				const data = resp?.data ?? resp ?? {};
				setStatusCounts({
					PENDING: Number(data.PENDING || 0),
					PENDING_UPDATE: Number(data.PENDING_UPDATE || 0),
					ACTIVE: Number(data.ACTIVE || 0),
					REJECTED: Number(data.REJECTED || 0),
					INACTIVE: Number(data.INACTIVE || 0),
				});
			} catch (e) {
				// 상태 카운트는 보조 정보라 실패해도 목록 사용은 유지합니다.
			}
		};
		loadCounts();
	}, []);

	const filteredList = useMemo(() => {
		let next = products || [];
		if (statusFilter) {
			next = statusFilter === 'PENDING_APPROVAL'
				? next.filter((product) => product.productActiveStatus === approvalSubFilter)
				: next.filter((product) => product.productActiveStatus === statusFilter);
		}
		if (typeFilter !== 'ALL') {
			next = next.filter((product) => product.productType === typeFilter);
		}
		if (searchText) {
			next = utils.wildCardSearch(next, searchText);
		}
		return next;
	}, [products, statusFilter, approvalSubFilter, typeFilter, searchText]);

	const summaryCounts = useMemo(() => {
		const rows = products || [];
		return {
			total: rows.length,
			active: rows.filter((item) => item.productActiveStatus === 'ACTIVE').length,
			pending: rows.filter((item) => ['PENDING', 'PENDING_UPDATE'].includes(item.productActiveStatus)).length,
			rejected: rows.filter((item) => item.productActiveStatus === 'REJECTED').length,
		};
	}, [products]);

	const handleRefresh = () => {
		dispatch(fetchMyProducts());
	};

	const handleEditProduct = (product) => {
		const editableStatuses = ['PENDING', 'REJECTED', 'ACTIVE', 'PENDING_UPDATE', 'INACTIVE'];
		if (!editableStatuses.includes(product.productActiveStatus)) {
			message.warning('수정 가능한 상태가 아닙니다.');
			return;
		}
		navigate(`/app/partner/products/edit/${product.productNo}`);
	};

	const handleCancelUpdate = (product) => {
		Modal.confirm({
			title: '수정 신청 취소',
			content: `"${product.productName}" 상품의 수정 신청을 취소하시겠습니까? 취소하면 원래 상태로 복구됩니다.`,
			okText: '취소 신청',
			okType: 'danger',
			cancelText: '닫기',
			onOk: async () => {
				try {
					await dispatch(cancelProductUpdate(product.productNo)).unwrap();
					message.success('수정 신청이 취소되었습니다.');
					dispatch(fetchMyProducts());
				} catch (err) {
					message.error(err || '수정 신청 취소에 실패했습니다.');
				}
			},
		});
	};

	const handleDeleteProduct = (product) => {
		const isPending = product.productActiveStatus === 'PENDING';
		Modal.confirm({
			title: isPending ? '등록 신청 취소' : '상품 비활성화',
			content: isPending
				? `"${product.productName}" 상품의 등록 신청을 취소하시겠습니까? 취소하면 상품이 완전히 삭제됩니다.`
				: `"${product.productName}" 상품을 비활성화하시겠습니까?`,
			okText: isPending ? '등록 신청 취소' : '비활성화',
			okType: 'danger',
			cancelText: '닫기',
			onOk: async () => {
				try {
					await dispatch(deleteProduct(product.productNo)).unwrap();
					message.success(isPending ? '등록 신청이 취소되었습니다.' : '상품이 비활성화되었습니다.');
					dispatch(fetchMyProducts());
				} catch (err) {
					message.error(err || (isPending ? '등록 신청 취소에 실패했습니다.' : '상품 비활성화에 실패했습니다.'));
				}
			},
		});
	};

	const expandedRowRender = (record) => (
		<Table
			columns={[
				{ title: '옵션 번호', dataIndex: 'optionNo', key: 'optionNo', width: 120 },
				{ title: '색상', dataIndex: 'color', key: 'color', render: (color) => color || '-' },
				{ title: '사이즈', dataIndex: 'size', key: 'size', render: (size) => size || '-' },
				{
					title: '추가 가격',
					dataIndex: 'optionAddPrice',
					key: 'optionAddPrice',
					render: (price) => Number(price || 0) === 0 ? <Tag color="green">기본 가격</Tag> : <span>+{formatPrice(price)}</span>,
				},
			]}
			dataSource={record.options || []}
			pagination={false}
			rowKey="optionNo"
			size="small"
			locale={{ emptyText: '옵션이 없습니다.' }}
		/>
	);

	const dropdownMenu = (record) => (
		<Menu>
			<Menu.Item onClick={() => handleEditProduct(record)}>
				<Flex alignItems="center">
					<EditOutlined />
					<span className="ml-2">수정</span>
				</Flex>
			</Menu.Item>
			{record.productActiveStatus === 'PENDING_UPDATE' && (
				<Menu.Item onClick={() => handleCancelUpdate(record)}>
					<Flex alignItems="center">
						<ReloadOutlined />
						<span className="ml-2">취소 신청</span>
					</Flex>
				</Menu.Item>
			)}
			{['ACTIVE', 'PENDING'].includes(record.productActiveStatus) && (
				<Menu.Item onClick={() => handleDeleteProduct(record)} danger>
					<Flex alignItems="center">
						<DeleteOutlined />
						<span className="ml-2">{record.productActiveStatus === 'PENDING' ? '등록 신청 취소' : '비활성화'}</span>
					</Flex>
				</Menu.Item>
			)}
		</Menu>
	);

	const tableColumns = [
		{
			title: '상품',
			dataIndex: 'productName',
			key: 'productName',
			render: (text, record) => {
				const status = STATUS_META[record.productActiveStatus] || { label: record.productActiveStatus || '-', color: 'default' };
				return (
					<div className="d-flex">
						<AvatarStatus size={72} type="square" src={record.productImageUrl} />
						<div className="ml-3">
							<div className="font-weight-semibold d-flex align-items-center" style={{ gap: 8 }}>
								<span>{text}</span>
								<Tag color={status.color}>{status.label}</Tag>
							</div>
							<div className="text-muted" style={{ fontSize: 12 }}>
								#{record.productNo} · {PRODUCT_TYPE_LABELS[record.productType] || record.productType || '-'}
								{record.productSubType && record.productSubType !== 'NONE' ? ` / ${record.productSubType}` : ''}
							</div>
							{record.rejectionReason && record.productActiveStatus !== 'PENDING_UPDATE' && (
								<Tooltip title={record.rejectionReason}>
									<Tag color="red" className="mt-2">거절 사유 있음</Tag>
								</Tooltip>
							)}
						</div>
					</div>
				);
			},
			sorter: (a, b) => utils.antdTableSorter(a, b, 'productName'),
		},
		{
			title: '가격',
			dataIndex: 'productPrice',
			key: 'productPrice',
			render: (price) => <span className="font-weight-semibold">{formatPrice(price)}</span>,
			sorter: (a, b) => Number(a.productPrice || 0) - Number(b.productPrice || 0),
		},
		{
			title: '옵션',
			key: 'optionCount',
			render: (_, record) => <Tag color="blue">{record.options?.length || 0}개</Tag>,
		},
		{
			title: '상태',
			dataIndex: 'productActiveStatus',
			key: 'productActiveStatus',
			render: (status) => {
				const meta = STATUS_META[status] || { label: status || '-', badge: 'default' };
				return <Badge status={meta.badge} text={meta.label} />;
			},
		},
		{
			title: '',
			key: 'action',
			width: 80,
			render: (_, record) => (
				<div className="text-right">
					<EllipsisDropdown menu={dropdownMenu(record)} />
				</div>
			),
		},
	];

	return (
		<>
			<Row gutter={16}>
				<Col xs={12} md={6}>
					<Card><span className="text-muted">전체 상품</span><h2 className="mb-0">{summaryCounts.total}</h2></Card>
				</Col>
				<Col xs={12} md={6}>
					<Card><span className="text-muted">판매 가능</span><h2 className="mb-0">{summaryCounts.active}</h2></Card>
				</Col>
				<Col xs={12} md={6}>
					<Card><span className="text-muted">승인 대기</span><h2 className="mb-0">{summaryCounts.pending}</h2></Card>
				</Col>
				<Col xs={12} md={6}>
					<Card><span className="text-muted">거절</span><h2 className="mb-0">{summaryCounts.rejected}</h2></Card>
				</Col>
			</Row>

			<Card>
				<Flex alignItems="center" justifyContent="space-between" mobileFlex={false}>
					<Flex className="mb-1" mobileFlex={false}>
						<div className="mr-md-3 mb-3">
							<Input
								placeholder="상품명, 번호, 설명 검색"
								prefix={<SearchOutlined />}
								onChange={(e) => setSearchText(e.currentTarget.value)}
								value={searchText}
								allowClear
							/>
						</div>
						<div className="mr-md-3 mb-3">
							<Select value={typeFilter} style={{ minWidth: 180 }} onChange={setTypeFilter}>
								<Option value="ALL">전체 카테고리</Option>
								{Object.entries(PRODUCT_TYPE_LABELS).map(([value, label]) => (
									<Option key={value} value={value}>{label}</Option>
								))}
							</Select>
						</div>
						<div className="mb-3">
							<Button icon={<ReloadOutlined />} onClick={handleRefresh} loading={loading}>
								새로고침
							</Button>
						</div>
					</Flex>
					<div>
						<Button type="primary" icon={<PlusCircleOutlined />} onClick={() => navigate('/app/partner/products/create')} block>
							상품 등록 신청
						</Button>
					</div>
				</Flex>

				<Tabs
					activeKey={statusFilter || 'ALL'}
					items={[
						{ key: 'ALL', label: '전체' },
						{
							key: 'PENDING_APPROVAL',
							label: `승인 대기 (${(statusCounts.PENDING || 0) + (statusCounts.PENDING_UPDATE || 0)})`,
							children: (
								<Tabs
									type="card"
									activeKey={approvalSubFilter}
									items={[
										{ key: 'PENDING', label: `등록 (${statusCounts.PENDING || 0})` },
										{ key: 'PENDING_UPDATE', label: `수정 (${statusCounts.PENDING_UPDATE || 0})` },
									]}
									onChange={setApprovalSubFilter}
									style={{ marginBottom: 16 }}
								/>
							),
						},
						{ key: 'ACTIVE', label: `활성 (${statusCounts.ACTIVE || 0})` },
						{ key: 'REJECTED', label: `거절 (${statusCounts.REJECTED || 0})` },
						{ key: 'INACTIVE', label: `비활성 (${statusCounts.INACTIVE || 0})` },
					]}
					onChange={(key) => {
						if (key === 'ALL') {
							setStatusFilter(null);
						} else if (key === 'PENDING_APPROVAL') {
							setStatusFilter('PENDING_APPROVAL');
							setApprovalSubFilter('PENDING');
						} else {
							setStatusFilter(key);
						}
					}}
					style={{ marginBottom: 16 }}
				/>

				{error && <Alert message="오류" description={error} type="error" showIcon className="mb-3" closable />}

				<div className="table-responsive">
					<Spin spinning={loading}>
						<Table
							columns={tableColumns}
							dataSource={filteredList}
							rowKey="productNo"
							expandable={{
								expandedRowRender,
								rowExpandable: (record) => record.options && record.options.length > 0,
							}}
							pagination={{
								pageSize: 10,
								showSizeChanger: true,
								showTotal: (total) => `총 ${total}개 상품`,
							}}
							locale={{ emptyText: '등록된 상품이 없습니다.' }}
						/>
					</Spin>
				</div>
			</Card>
		</>
	);
};

export default PartnerProducts;
