import React, { useEffect, useState, useMemo } from 'react';
import { Card, Table, Input, Button, Badge, Tag, Spin, Alert, message, Menu, Modal, Tabs } from 'antd';
import { SearchOutlined, PlusCircleOutlined, ReloadOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import AvatarStatus from 'components/shared-components/AvatarStatus';
import Flex from 'components/shared-components/Flex';
import EllipsisDropdown from 'components/shared-components/EllipsisDropdown';
import { useDispatch, useSelector } from 'react-redux';
import { fetchMyProducts, createProduct, updateProduct, deleteProduct, cancelProductUpdate } from 'store/slices/partnerSlice';
import ProductCreateModal from './ProductCreateModal';
import ProductEditModal from './ProductEditModal';
import utils from 'utils';
import PartnerService from 'services/PartnerService';

const PartnerProducts = () => {
	const dispatch = useDispatch();
	const { products, loading, error } = useSelector((state) => state.partner);
	const [filteredList, setFilteredList] = useState([]);
	const [searchText, setSearchText] = useState('');
	const [statusFilter, setStatusFilter] = useState(null); // null = 전체, 'PENDING' = 승인 대기, 'ACTIVE' = 활성, etc.
	const [approvalSubFilter, setApprovalSubFilter] = useState('PENDING'); // 승인 대기 탭 내부 필터: 'PENDING' 또는 'PENDING_UPDATE'
	const [modalVisible, setModalVisible] = useState(false);
	const [editModalVisible, setEditModalVisible] = useState(false);
	const [selectedProduct, setSelectedProduct] = useState(null);
	// 상태별 요약 카운트
	const [statusCounts, setStatusCounts] = useState({ PENDING: 0, PENDING_UPDATE: 0, ACTIVE: 0, REJECTED: 0, INACTIVE: 0 });

	// 컴포넌트 마운트 시 상품 목록 조회
	useEffect(() => {
		dispatch(fetchMyProducts());
	}, [dispatch]);
	// 요약 카운트 조회
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
				// 무시: 필터링 계산으로 대체
			}
		};
		loadCounts();
	}, []);

	// products가 변경되거나 상태 필터가 변경되면 filteredList 업데이트
	useEffect(() => {
		let filtered = products;
		
		// 상태 필터 적용
		if (statusFilter) {
			if (statusFilter === 'PENDING_APPROVAL') {
				// 승인 대기 탭: 서브필터 적용
				filtered = filtered.filter(p => p.productActiveStatus === approvalSubFilter);
			} else {
				filtered = filtered.filter(p => p.productActiveStatus === statusFilter);
			}
		}
		
		// 검색 필터 적용
		if (searchText) {
			filtered = utils.wildCardSearch(filtered, searchText);
		}
		
		setFilteredList(filtered);
	}, [products, statusFilter, approvalSubFilter, searchText]);

	// 검색 기능
	const onSearch = (e) => {
		const value = e.currentTarget.value;
		setSearchText(value);
	};

	// 새로고침
	const handleRefresh = () => {
		dispatch(fetchMyProducts());
	};

		// 상품 등록 신청 핸들러
	const handleCreateProduct = async (productData) => {
		try {
			const result = await dispatch(createProduct(productData)).unwrap();
			message.success('상품 등록 신청이 완료되었습니다. 관리자 승인 후 판매가 시작됩니다.');
			setModalVisible(false);
			// 목록 새로고침 (등록된 상품이 서버에 반영되도록 약간의 지연 후 호출)
			setTimeout(async () => {
				await dispatch(fetchMyProducts()).unwrap();
			}, 100);
		} catch (err) {
			message.error(err || '상품 등록 신청에 실패했습니다.');
		}
	};

	// 상품 수정 핸들러
	const handleEditProduct = (product) => {
		// PENDING, REJECTED, ACTIVE, PENDING_UPDATE, INACTIVE 상태인 상품만 수정 가능
		const editableStatuses = ['PENDING', 'REJECTED', 'ACTIVE', 'PENDING_UPDATE', 'INACTIVE'];
		if (!editableStatuses.includes(product.productActiveStatus)) {
			message.warning('수정 가능한 상태가 아닙니다. (수정 가능: 승인 대기, 거절, 활성, 수정 승인 대기, 비활성)');
			return;
		}
		setSelectedProduct(product);
		setEditModalVisible(true);
	};

	// 상품 수정 및 재신청 핸들러
	const handleUpdateProduct = async (productData) => {
		try {
			const result = await dispatch(updateProduct({ 
				productNo: selectedProduct.productNo, 
				productData 
			})).unwrap();
			
			// 상태에 따라 다른 성공 메시지 표시
			const currentStatus = selectedProduct.productActiveStatus;
			let successMessage = '';
			
			if (currentStatus === 'REJECTED') {
				successMessage = '상품이 수정되었고 재신청되었습니다. 관리자 승인 후 판매가 시작됩니다.';
			} else if (currentStatus === 'PENDING') {
				successMessage = '상품이 수정되었습니다. 관리자 승인 후 판매가 시작됩니다.';
			} else if (currentStatus === 'ACTIVE') {
				// ACTIVE 상태에서 수정 시, 결과 상태에 따라 메시지 분기
				const newStatus = result?.productActiveStatus || 'ACTIVE';
				if (newStatus === 'PENDING_UPDATE') {
					successMessage = '상품이 수정되었습니다. 중요한 항목 변경으로 인해 관리자 승인이 필요합니다.';
				} else {
					successMessage = '상품이 수정되었습니다. 변경 사항이 즉시 반영되었습니다.';
				}
			} else if (currentStatus === 'PENDING_UPDATE') {
				successMessage = '상품 수정 내용이 업데이트되었습니다. 관리자 승인 후 변경 사항이 반영됩니다.';
			} else if (currentStatus === 'INACTIVE') {
				const newStatus = result?.productActiveStatus || 'INACTIVE';
				if (newStatus === 'PENDING' || newStatus === 'PENDING_UPDATE') {
					successMessage = '상품이 수정되었습니다. 관리자 승인 후 재활성화됩니다.';
				} else {
					successMessage = '상품이 수정되었습니다.';
				}
			} else {
				successMessage = '상품이 수정되었습니다.';
			}
			
			message.success(successMessage);
			setEditModalVisible(false);
			setSelectedProduct(null);
			// 목록 새로고침
			setTimeout(async () => {
				await dispatch(fetchMyProducts()).unwrap();
			}, 100);
		} catch (err) {
			message.error(err || '상품 수정에 실패했습니다.');
		}
	};

	// 수정 신청 취소 핸들러
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
					// 목록 새로고침
					setTimeout(async () => {
						await dispatch(fetchMyProducts()).unwrap();
					}, 100);
				} catch (err) {
					message.error(err || '수정 신청 취소에 실패했습니다.');
				}
			}
		});
	};

	// 상품 삭제 핸들러
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
					const isPending = product.productActiveStatus === 'PENDING';
					await dispatch(deleteProduct(product.productNo)).unwrap();
					message.success(isPending ? '등록 신청이 취소되었습니다.' : '상품이 비활성화되었습니다.');
					// 목록 새로고침
					setTimeout(async () => {
						await dispatch(fetchMyProducts()).unwrap();
					}, 100);
				} catch (err) {
					const isPendingError = product.productActiveStatus === 'PENDING';
					message.error(err || (isPendingError ? '등록 신청 취소에 실패했습니다.' : '상품 비활성화에 실패했습니다.'));
				}
			}
		});
	};

	// 옵션 표시 (expandable row)
	const expandedRowRender = (record) => {
		const columns = [
			{
				title: '옵션 번호',
				dataIndex: 'optionNo',
				key: 'optionNo',
				width: 100,
			},
			{
				title: '색상',
				dataIndex: 'color',
				key: 'color',
				render: (color) => color || '-',
			},
			{
				title: '사이즈',
				dataIndex: 'size',
				key: 'size',
				render: (size) => size || '-',
			},
			{
				title: '옵션 정보',
				key: 'optionInfo',
				render: (_, option) => {
					const parts = [];
					if (option.color) parts.push(`색상: ${option.color}`);
					if (option.size) parts.push(`사이즈: ${option.size}`);
					return parts.length > 0 ? parts.join(' / ') : '옵션 없음';
				},
			},
			{
				title: '추가 가격',
				dataIndex: 'optionAddPrice',
				key: 'optionAddPrice',
				render: (price) => {
					if (price === 0 || price === null || price === undefined) {
						return <Tag color="green">기본 가격</Tag>;
					}
					return (
						<span style={{ color: '#ff4d4f' }}>
							+{parseInt(price).toLocaleString()}원
						</span>
					);
				},
			},
		];

		return (
			<Table
				columns={columns}
				dataSource={record.options || []}
				pagination={false}
				rowKey="optionNo"
				size="small"
				locale={{
					emptyText: '옵션이 없습니다.',
				}}
			/>
		);
	};

	// 테이블 컬럼 정의 (상태 필터에 따라 동적으로 변경)
	const tableColumns = useMemo(() => [
		{
			title: '상품번호',
			dataIndex: 'productNo',
			key: 'productNo',
			width: 100,
		},
		{
			title: '상품명',
			dataIndex: 'productName',
			key: 'productName',
			render: (text, record) => (
				<div className="d-flex">
					<AvatarStatus
						size={60}
						type="square"
						src={record.productImageUrl}
						// name prop 제거 (외부에서 상품명 표시)
					/>
					<div className="ml-2">
						<div className="font-weight-semibold" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
							{text}
							{/* 거절 사유가 있는 경우 배지 표시 (PENDING_UPDATE 제외) */}
							{record.rejectionReason && record.productActiveStatus !== 'PENDING_UPDATE' && (
								<Tag color="red" style={{ margin: 0 }}>
									{record.productActiveStatus === 'REJECTED' ? '거절' : '수정 거절'}
								</Tag>
							)}
						</div>
						<div className="text-muted" style={{ fontSize: '12px' }}>
							{record.productType} / {record.productSubType === 'NONE' || !record.productSubType ? '없음' : record.productSubType}
						</div>
					</div>
				</div>
			),
		},
		{
			title: '가격',
			dataIndex: 'productPrice',
			key: 'productPrice',
			render: (price) => (
				<span className="font-weight-semibold">
					{parseInt(price).toLocaleString()}원
				</span>
			),
		},
		{
			title: '옵션 수',
			key: 'optionCount',
			render: (_, record) => (
				<Tag color="blue">{record.options?.length || 0}개</Tag>
			),
		},
		// 상태 컬럼: 전체 탭일 때만 표시
		...(statusFilter === null ? [{
			title: '상태',
			dataIndex: 'productActiveStatus',
			key: 'productActiveStatus',
			render: (status, record) => {
				const statusBadge = (() => {
					switch (status) {
						case 'PENDING':
							return <Badge status="processing" text="승인 대기 중" />;
					case 'ACTIVE':
						return <Badge status="success" text="활성" />;
					case 'REJECTED':
						return <Badge status="error" text="거절" />;
						case 'INACTIVE':
							return <Badge status="default" text="비활성" />;
						case 'PENDING_UPDATE':
							return <Badge status="warning" text="수정 승인 대기" />;
						default:
							return <Badge status="default" text={status || '-'} />;
					}
				})();
				
				// 거절 사유가 있는 경우 표시
				// REJECTED 상태: 신규 등록 거절
				// ACTIVE 상태 + rejectionReason: 수정 거절 (원래 상태로 복구되었지만 거절 사유 저장됨)
				// PENDING_UPDATE 상태에서는 이전 거절 사유를 숨김 (새로운 수정 요청이므로)
				if (record.rejectionReason && status !== 'PENDING_UPDATE') {
					const rejectionLabel = status === 'REJECTED' 
						? '거절 사유' 
						: '수정 거절 사유';
					return (
						<div>
							{statusBadge}
							<div style={{ marginTop: 4, fontSize: '12px', color: '#ff4d4f' }}>
								{rejectionLabel}: {record.rejectionReason}
							</div>
						</div>
					);
				}
					
					return statusBadge;
				},
			}] : []),
		{
			title: '작업',
			key: 'action',
			width: 100,
			render: (_, record) => {
				const menu = (
					<Menu>
						<Menu.Item onClick={() => handleEditProduct(record)}>
							<Flex alignItems="center">
								<EditOutlined />
								<span className="ml-2">수정</span>
							</Flex>
						</Menu.Item>
						{/* PENDING_UPDATE 상태일 때는 삭제 버튼 대신 수정 신청 취소 버튼 표시 */}
						{record.productActiveStatus === 'PENDING_UPDATE' ? (
							<Menu.Item onClick={() => handleCancelUpdate(record)}>
								<Flex alignItems="center">
									<ReloadOutlined />
									<span className="ml-2">취소 신청</span>
								</Flex>
							</Menu.Item>
						) : (record.productActiveStatus === 'ACTIVE' || record.productActiveStatus === 'PENDING') ? (
							<Menu.Item 
								onClick={() => handleDeleteProduct(record)}
								danger
							>
								<Flex alignItems="center">
									<DeleteOutlined />
									<span className="ml-2">{record.productActiveStatus === 'PENDING' ? '등록 신청 취소' : '비활성화'}</span>
								</Flex>
							</Menu.Item>
						) : null}
					</Menu>
				);
				return <EllipsisDropdown menu={menu} />;
			},
		},
	], [statusFilter]);

	return (
		<Card>
			<Flex alignItems="center" justifyContent="space-between" mobileFlex={false}>
				<Flex className="mb-1" mobileFlex={false}>
					<div className="mr-md-3 mb-3">
						<Input
							placeholder="상품명 검색"
							prefix={<SearchOutlined />}
							onChange={onSearch}
							value={searchText}
							allowClear
						/>
					</div>
					<div className="mb-3">
						<Button
							icon={<ReloadOutlined />}
							onClick={handleRefresh}
							loading={loading}
						>
							새로고침
						</Button>
					</div>
				</Flex>
				<div>
					<Button
						type="primary"
						icon={<PlusCircleOutlined />}
						onClick={() => setModalVisible(true)}
						block
					>
						상품 등록 신청
					</Button>
				</div>
			</Flex>

			{/* 상태별 필터 탭 */}
			<Tabs
				activeKey={statusFilter || 'ALL'}
				items={[
					{
						key: 'ALL',
						label: '전체',
					},
					{
						key: 'PENDING_APPROVAL',
						label: `승인 대기 (${(statusCounts.PENDING || 0) + (statusCounts.PENDING_UPDATE || 0)})`,
						children: (
							<Tabs
								type="card"
								activeKey={approvalSubFilter}
								items={[
									{
										key: 'PENDING',
										label: `등록 (${statusCounts.PENDING || 0})`,
									},
									{
										key: 'PENDING_UPDATE',
										label: `수정 (${statusCounts.PENDING_UPDATE || 0})`,
									},
								]}
								onChange={(key) => setApprovalSubFilter(key)}
								style={{ marginBottom: 16 }}
							/>
						),
					},
					{
						key: 'ACTIVE',
						label: `활성 (${statusCounts.ACTIVE || 0})`,
					},
					{
						key: 'REJECTED',
						label: `거절 (${statusCounts.REJECTED || 0})`,
					},
					{
						key: 'INACTIVE',
						label: `비활성 (${statusCounts.INACTIVE || 0})`,
					},
				]}
				onChange={(key) => {
					if (key === 'ALL') {
						setStatusFilter(null);
					} else if (key === 'PENDING_APPROVAL') {
						setStatusFilter('PENDING_APPROVAL');
						setApprovalSubFilter('PENDING'); // 기본값으로 등록 탭 선택
					} else {
						setStatusFilter(key);
					}
				}}
				style={{ marginBottom: 16 }}
			/>

			{error && (
				<Alert
					message="오류"
					description={error}
					type="error"
					showIcon
					className="mb-3"
					closable
				/>
			)}

			<div className="table-responsive">
				<Spin spinning={loading}>
					<Table
						columns={tableColumns}
						dataSource={filteredList}
						rowKey="productNo"
						// 거절 사유가 있는 행 강조 (PENDING_UPDATE 제외)
						rowClassName={(record) => {
							if (record.rejectionReason && record.productActiveStatus !== 'PENDING_UPDATE') {
								return 'rejected-row';
							}
							return '';
						}}
						expandable={{
							expandedRowRender,
							rowExpandable: (record) =>
								record.options && record.options.length > 0,
						}}
						pagination={{
							pageSize: 10,
							showSizeChanger: true,
							showTotal: (total) => `총 ${total}개 상품`,
						}}
						locale={{
							emptyText: '등록된 상품이 없습니다.',
						}}
					/>
				</Spin>
			</div>
			
			{/* 거절된 행 강조 스타일 */}
			<style>{`
				.rejected-row {
					background-color: #fff1f0 !important;
				}
				.rejected-row:hover {
					background-color: #ffe7e5 !important;
				}
			`}</style>

			<ProductCreateModal
				visible={modalVisible}
				onCancel={() => setModalVisible(false)}
				onSubmit={handleCreateProduct}
				loading={loading}
			/>

			<ProductEditModal
				visible={editModalVisible}
				onCancel={() => {
					setEditModalVisible(false);
					setSelectedProduct(null);
				}}
				onSubmit={handleUpdateProduct}
				loading={loading}
				product={selectedProduct}
			/>
		</Card>
	);
};

export default PartnerProducts;
