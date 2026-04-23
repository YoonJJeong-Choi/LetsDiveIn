import React, { useEffect, useState } from 'react';
import { Card, Table, Input, Button, Badge, Tag, Spin, Alert, message, Modal, InputNumber, Form, Select } from 'antd';
import { SearchOutlined, ReloadOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import Flex from 'components/shared-components/Flex';
import { useDispatch, useSelector } from 'react-redux';
import { 
	fetchMyInventories, 
	updateInventory, 
	updateInventoryForProduct,
	createInventory,
	createInventoryForProduct
} from 'store/slices/inventorySlice';
import { fetchMyProducts } from 'store/slices/partnerSlice';
import utils from 'utils';

const PartnerInventory = () => {
	const dispatch = useDispatch();
	const { inventories, loading, error } = useSelector((state) => state.inventory);
	const { products } = useSelector((state) => state.partner);
	const [filteredList, setFilteredList] = useState([]);
	const [searchText, setSearchText] = useState('');
	const [editModalVisible, setEditModalVisible] = useState(false);
	const [createModalVisible, setCreateModalVisible] = useState(false);
	const [selectedInventory, setSelectedInventory] = useState(null);
	const [form] = Form.useForm();
	const [createForm] = Form.useForm();

	// 컴포넌트 마운트 시 재고 목록 및 상품 목록 조회
	useEffect(() => {
		dispatch(fetchMyInventories());
		dispatch(fetchMyProducts());
	}, [dispatch]);

	// inventories가 변경되거나 검색어가 변경되면 filteredList 업데이트
	useEffect(() => {
		// inventories가 배열인지 확인
		const inventoryList = Array.isArray(inventories) ? inventories : [];
		let filtered = inventoryList;
		
		// 검색 필터 적용
		if (searchText && Array.isArray(filtered)) {
			filtered = utils.wildCardSearch(filtered, searchText);
		}
		
		setFilteredList(Array.isArray(filtered) ? filtered : []);
	}, [inventories, searchText]);

	// 검색 기능
	const onSearch = (e) => {
		const value = e.currentTarget.value;
		setSearchText(value);
	};

	// 새로고침
	const handleRefresh = () => {
		dispatch(fetchMyInventories());
	};

	// 재고 수정 모달 열기
	const handleEditInventory = (inventory) => {
		setSelectedInventory(inventory);
		form.setFieldsValue({
			stockQuantity: inventory.stockQuantity || 0
		});
		setEditModalVisible(true);
	};

	// 재고 수정 핸들러
	const handleUpdateInventory = async (values) => {
		try {
			const { stockQuantity } = values;
			const inventory = selectedInventory;
			
			if (inventory.optionNo) {
				// 옵션이 있는 상품
				await dispatch(updateInventory({
					optionNo: inventory.optionNo,
					inventoryData: { stockQuantity }
				})).unwrap();
			} else {
				// 옵션이 없는 상품
				await dispatch(updateInventoryForProduct({
					productNo: inventory.productNo,
					inventoryData: { stockQuantity }
				})).unwrap();
			}
			
			message.success('재고가 수정되었습니다.');
			setEditModalVisible(false);
			setSelectedInventory(null);
			form.resetFields();
			dispatch(fetchMyInventories());
		} catch (err) {
			message.error(err || '재고 수정에 실패했습니다.');
		}
	};

	// 재고 등록 모달 열기
	const handleCreateInventory = () => {
		createForm.resetFields();
		setCreateModalVisible(true);
	};

	// 재고 등록 핸들러
	const handleCreateInventorySubmit = async (values) => {
		try {
			const { productNo, optionNo, stockQuantity } = values;
			
			if (optionNo) {
				// 옵션이 있는 상품
				await dispatch(createInventory({
					optionNo: optionNo,
					inventoryData: { stockQuantity }
				})).unwrap();
			} else {
				// 옵션이 없는 상품
				await dispatch(createInventoryForProduct({
					productNo: productNo,
					inventoryData: { stockQuantity }
				})).unwrap();
			}
			
			message.success('재고가 등록되었습니다.');
			setCreateModalVisible(false);
			createForm.resetFields();
			dispatch(fetchMyInventories());
		} catch (err) {
			message.error(err || '재고 등록에 실패했습니다.');
		}
	};

	// 상품 목록에서 재고가 등록되지 않은 항목만 필터링
	const getAvailableProductsForInventory = () => {
		if (!Array.isArray(products)) return [];
		
		const registeredInventoryKeys = new Set();
		if (Array.isArray(inventories)) {
			inventories.forEach(inv => {
				if (inv.optionNo) {
					registeredInventoryKeys.add(`option-${inv.optionNo}`);
				} else {
					registeredInventoryKeys.add(`product-${inv.productNo}`);
				}
			});
		}

		const availableItems = [];
		products.forEach(product => {
			// ACTIVE 또는 PENDING_UPDATE 상태인 상품만 표시
			if (product.productActiveStatus !== 'ACTIVE' && product.productActiveStatus !== 'PENDING_UPDATE') {
				return;
			}

			if (product.options && product.options.length > 0) {
				// 옵션이 있는 상품
				product.options.forEach(option => {
					if (!registeredInventoryKeys.has(`option-${option.optionNo}`)) {
						availableItems.push({
							key: `option-${option.optionNo}`,
							label: `${product.productName} - ${option.color || ''} ${option.size || ''}`,
							productNo: product.productNo,
							productName: product.productName,
							optionNo: option.optionNo,
							color: option.color,
							size: option.size,
							type: 'option'
						});
					}
				});
			} else {
				// 옵션이 없는 상품
				if (!registeredInventoryKeys.has(`product-${product.productNo}`)) {
					availableItems.push({
						key: `product-${product.productNo}`,
						label: `${product.productName} (단일 상품)`,
						productNo: product.productNo,
						productName: product.productName,
						optionNo: null,
						type: 'product'
					});
				}
			}
		});

		return availableItems;
	};


	// 테이블 컬럼 정의
	const tableColumns = [
		{
			title: '상품명',
			dataIndex: 'productName',
			key: 'productName',
			render: (text) => <span className="font-weight-semibold">{text}</span>
		},
		{
			title: '옵션',
			key: 'option',
			render: (_, record) => {
				if (record.optionNo) {
					return (
						<span>
							{record.color && <Tag>{record.color}</Tag>}
							{record.size && <Tag>{record.size}</Tag>}
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
			render: (quantity) => (
				<span className={quantity === 0 ? 'text-danger' : ''}>
					{quantity}개
				</span>
			)
		},
		{
			title: '재고 상태',
			dataIndex: 'inStock',
			key: 'inStock',
			render: (inStock) => (
				<Badge 
					status={inStock ? 'success' : 'error'} 
					text={inStock ? '재고 있음' : '품절'}
				/>
			)
		},
		{
			title: '작업',
			key: 'action',
			render: (_, record) => (
				<Flex alignItems="center">
					<Button
						type="link"
						icon={<EditOutlined />}
						onClick={() => handleEditInventory(record)}
					>
						수정
					</Button>
				</Flex>
			)
		}
	];

	return (
		<>
			<Card>
				<Flex alignItems="center" justifyContent="between" mobileFlex={false}>
					<Flex className="mb-1" mobileFlex={false}>
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
							className="mr-2"
						>
							새로고침
						</Button>
						<Button
							type="primary"
							icon={<PlusOutlined />}
							onClick={handleCreateInventory}
						>
							재고 등록
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
							showTotal: (total) => `총 ${total}개`
						}}
					/>
				</Spin>
			</Card>

			{/* 재고 수정 모달 */}
			<Modal
				title="재고 수정"
				open={editModalVisible}
				onOk={() => form.submit()}
				onCancel={() => {
					setEditModalVisible(false);
					setSelectedInventory(null);
					form.resetFields();
				}}
				okText="수정"
				cancelText="닫기"
			>
				{selectedInventory && (
					<>
						<Alert
							message={`${selectedInventory.productName}${selectedInventory.optionNo ? ` (${selectedInventory.color || ''} ${selectedInventory.size || ''})` : ' (단일 상품)'}`}
							type="info"
							showIcon
							className="mb-3"
						/>
						<Form
							form={form}
							layout="vertical"
							onFinish={handleUpdateInventory}
						>
							<Form.Item
								label="재고 수량"
								name="stockQuantity"
								rules={[
									{ required: true, message: '재고 수량을 입력해주세요.' },
									{ type: 'number', min: 0, message: '재고 수량은 0 이상이어야 합니다.' }
								]}
							>
								<InputNumber
									min={0}
									style={{ width: '100%' }}
									placeholder="재고 수량을 입력하세요"
								/>
							</Form.Item>
						</Form>
					</>
				)}
			</Modal>

			{/* 재고 등록 모달 */}
			<Modal
				title="재고 등록"
				open={createModalVisible}
				onOk={() => createForm.submit()}
				onCancel={() => {
					setCreateModalVisible(false);
					createForm.resetFields();
				}}
				okText="등록"
				cancelText="닫기"
				width={600}
			>
				<Form
					form={createForm}
					layout="vertical"
					onFinish={handleCreateInventorySubmit}
				>
					<Form.Item
						label="상품/옵션 선택"
						name="itemKey"
						rules={[
							{ required: true, message: '상품 또는 옵션을 선택해주세요.' }
						]}
					>
						<Select
							placeholder="재고를 등록할 상품 또는 옵션을 선택하세요"
							showSearch
							filterOption={(input, option) =>
								(option?.label ?? '').toLowerCase().includes(input.toLowerCase())
							}
							onChange={(value) => {
								const availableItems = getAvailableProductsForInventory();
								const selectedItem = availableItems.find(item => item.key === value);
								if (selectedItem) {
									createForm.setFieldsValue({
										productNo: selectedItem.productNo,
										optionNo: selectedItem.optionNo,
										itemKey: value
									});
								}
							}}
						>
							{getAvailableProductsForInventory().map(item => (
								<Select.Option key={item.key} value={item.key}>
									{item.label}
								</Select.Option>
							))}
						</Select>
					</Form.Item>
					<Form.Item
						label="재고 수량"
						name="stockQuantity"
						rules={[
							{ required: true, message: '재고 수량을 입력해주세요.' },
							{ type: 'number', min: 0, message: '재고 수량은 0 이상이어야 합니다.' }
						]}
					>
						<InputNumber
							min={0}
							style={{ width: '100%' }}
							placeholder="재고 수량을 입력하세요"
						/>
					</Form.Item>
					<Form.Item name="productNo" hidden>
						<Input />
					</Form.Item>
					<Form.Item name="optionNo" hidden>
						<Input />
					</Form.Item>
				</Form>
			</Modal>

		</>
	);
};

export default PartnerInventory;
