import React, { useEffect, useState } from 'react';
import { Card, Table, Input, Button, Badge, Tag, Spin, Alert, Select } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import Flex from 'components/shared-components/Flex';
import { useDispatch, useSelector } from 'react-redux';
import { fetchAllInventories, fetchInventoriesByPartner, fetchAllPartners } from 'store/slices/adminSlice';
import utils from 'utils';

const { Option } = Select;

const AdminInventory = () => {
	const dispatch = useDispatch();
	const { allInventories, partnerInventories, allPartners, loading, error } = useSelector((state) => state.admin);
	const [filteredList, setFilteredList] = useState([]);
	const [searchText, setSearchText] = useState('');
	const [selectedPartnerId, setSelectedPartnerId] = useState(null);
	const [displayMode, setDisplayMode] = useState('all'); // 'all' or 'partner'

	// 컴포넌트 마운트 시 전체 재고 목록 및 파트너 목록 조회
	useEffect(() => {
		dispatch(fetchAllInventories());
		dispatch(fetchAllPartners({}));
	}, [dispatch]);

	// 선택된 파트너가 변경되면 해당 파트너의 재고 조회
	useEffect(() => {
		if (selectedPartnerId) {
			dispatch(fetchInventoriesByPartner(selectedPartnerId));
		}
	}, [selectedPartnerId, dispatch]);

	// inventories가 변경되거나 검색어가 변경되면 filteredList 업데이트
	useEffect(() => {
		const inventoryList = displayMode === 'all' 
			? (Array.isArray(allInventories) ? allInventories : [])
			: (Array.isArray(partnerInventories) ? partnerInventories : []);
		
		let filtered = inventoryList;
		
		if (searchText && Array.isArray(filtered)) {
			filtered = utils.wildCardSearch(filtered, searchText);
		}
		
		setFilteredList(Array.isArray(filtered) ? filtered : []);
	}, [allInventories, partnerInventories, searchText, displayMode]);

	// 검색 기능
	const onSearch = (e) => {
		const value = e.currentTarget.value;
		setSearchText(value);
	};

	// 새로고침
	const handleRefresh = () => {
		if (displayMode === 'all') {
			dispatch(fetchAllInventories());
		} else if (selectedPartnerId) {
			dispatch(fetchInventoriesByPartner(selectedPartnerId));
		}
	};

	// 파트너 선택 변경
	const handlePartnerChange = (partnerId) => {
		setSelectedPartnerId(partnerId);
		setDisplayMode(partnerId ? 'partner' : 'all');
	};

	// 테이블 컬럼 정의
	const tableColumns = [
		{
			title: '재고 번호',
			dataIndex: 'inventoryNo',
			key: 'inventoryNo',
			width: 100,
			sorter: (a, b) => (a.inventoryNo || 0) - (b.inventoryNo || 0),
		},
		{
			title: '상품 번호',
			dataIndex: 'productNo',
			key: 'productNo',
			width: 100,
			sorter: (a, b) => (a.productNo || 0) - (b.productNo || 0),
		},
		{
			title: '상품명',
			dataIndex: 'productName',
			key: 'productName',
			sorter: (a, b) => (a.productName || '').localeCompare(b.productName || ''),
			render: (text) => <span className="font-weight-semibold">{text}</span>
		},
		{
			title: '옵션',
			key: 'option',
			width: 200,
			render: (_, record) => {
				if (record.optionNo) {
					return (
						<span>
							{record.color && <Tag color="blue">{record.color}</Tag>}
							{record.size && <Tag color="green">{record.size}</Tag>}
							<div style={{ fontSize: '12px', color: '#999', marginTop: '4px' }}>
								옵션번호: {record.optionNo}
							</div>
						</span>
					);
				} else {
					return <Tag color="default">단일 상품</Tag>;
				}
			}
		},
		{
			title: '재고 수량',
			dataIndex: 'stockQuantity',
			key: 'stockQuantity',
			width: 120,
			sorter: (a, b) => (a.stockQuantity || 0) - (b.stockQuantity || 0),
			render: (quantity) => {
				const qty = quantity || 0;
				return (
					<span className={qty === 0 ? 'text-danger font-weight-bold' : qty < 10 ? 'text-warning' : ''}>
						{qty.toLocaleString()}개
					</span>
				);
			}
		},
		{
			title: '재고 상태',
			dataIndex: 'inStock',
			key: 'inStock',
			width: 120,
			filters: [
				{ text: '재고 있음', value: true },
				{ text: '품절', value: false }
			],
			onFilter: (value, record) => record.inStock === value,
			render: (inStock) => (
				<Badge
					status={inStock ? 'success' : 'error'}
					text={inStock ? '재고 있음' : '품절'}
				/>
			)
		}
	];

	return (
		<>
			<Card>
				<Flex alignItems="center" justifyContent="between" mobileFlex={false}>
					<Flex className="mb-1" mobileFlex={false}>
						<div className="mr-md-3 mb-3">
							<Select
								placeholder="파트너 선택 (전체 보기)"
								style={{ width: 200 }}
								allowClear
								onChange={handlePartnerChange}
								value={selectedPartnerId}
							>
								{allPartners.map(partner => (
									<Option key={partner.partnerId} value={partner.partnerId}>
										{partner.partnerName}
									</Option>
								))}
							</Select>
						</div>
						<div className="mr-md-3 mb-3">
							<Input
								placeholder="상품명으로 검색"
								prefix={<SearchOutlined />}
								onChange={onSearch}
								style={{ width: 300 }}
							/>
						</div>
					</Flex>
					<div>
						<Button
							type="default"
							icon={<ReloadOutlined />}
							onClick={handleRefresh}
							loading={loading}
						>
							새로고침
						</Button>
					</div>
				</Flex>
			</Card>

			<Card>
				{error && (
					<Alert
						message="오류"
						description={error}
						type="error"
						showIcon
						className="mb-3"
						closable
						onClose={() => {}}
					/>
				)}

				{!loading && filteredList.length === 0 && !error && (
					<Alert
						message="재고 정보가 없습니다"
						description={selectedPartnerId ? "선택한 파트너의 재고 정보가 없습니다." : "등록된 재고 정보가 없습니다."}
						type="info"
						showIcon
						className="mb-3"
					/>
				)}

				<Spin spinning={loading}>
					<Table
						columns={tableColumns}
						dataSource={filteredList}
						rowKey={(record) => record.inventoryNo || `${record.productNo}-${record.optionNo || 'no-option'}`}
						pagination={{
							pageSize: 10,
							showSizeChanger: true,
							pageSizeOptions: ['10', '20', '50', '100'],
							showTotal: (total, range) => `${range[0]}-${range[1]} / 총 ${total}개`
						}}
						scroll={{ x: 'max-content' }}
					/>
				</Spin>
			</Card>
		</>
	);
};

export default AdminInventory;
