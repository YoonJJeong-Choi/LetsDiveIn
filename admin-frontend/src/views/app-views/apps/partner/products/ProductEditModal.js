import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Select, Button, Space, message, Alert, Upload, Radio, List } from 'antd';
import { PlusOutlined, MinusCircleOutlined, ExclamationCircleOutlined, UploadOutlined } from '@ant-design/icons';
import FileService from 'services/FileService';
import AdminProductService from 'services/AdminProductService';

const { TextArea } = Input;
const { Option } = Select;

// 상품 대분류 옵션
const PRODUCT_TYPES = [
	{ value: 'SWIMSUIT_MEN', label: '남성 수영복' },
	{ value: 'SWIMSUIT_WOMEN', label: '여성 수영복' },
	{ value: 'SWIMSUIT_KIDS', label: '아동 수영복' },
	{ value: 'SWIM_CAP', label: '수영모자' },
	{ value: 'SWIM_GOGGLES', label: '수영안경' },
	{ value: 'FINS', label: '오리발' },
	{ value: 'SWIM_TOY', label: '수영용품' },
	{ value: 'ETC', label: '기타' },
];

// 상품 소분류 옵션 (대분류에 따라 필터링)
const PRODUCT_SUB_TYPES = {
	SWIMSUIT_WOMEN: [
		{ value: 'ONE_PIECE', label: '원피스' },
		{ value: 'BIKINI', label: '비키니' },
		{ value: 'MONOKINI', label: '모노키니' },
		{ value: 'RASH_GUARD', label: '래쉬가드' },
	],
	SWIMSUIT_MEN: [
		{ value: 'TRUNKS', label: '트렁크' },
		{ value: 'JAMMER', label: '잠머' },
		{ value: 'BRIEF', label: '브리프' },
	],
	SWIM_CAP: [
		{ value: 'CAP_SILICONE', label: '실리콘 수모' },
		{ value: 'CAP_FABRIC', label: '천 수모' },
	],
	FINS: [
		{ value: 'FINS_SHORT', label: '숏핀' },
		{ value: 'FINS_LONG', label: '롱핀' },
	],
};

const ProductEditModal = ({ visible, onCancel, onSubmit, loading, product }) => {
	const [form] = Form.useForm();
	const [selectedProductType, setSelectedProductType] = useState(null);
	const [imageItems, setImageItems] = useState([]); // {imageUrl, sortOrder, isPrimary, fileId?}

	useEffect(() => {
		if (visible && product) {
			// 기존 상품 데이터로 폼 초기화
			const subTypes = PRODUCT_SUB_TYPES[product.productType] || [];
			setSelectedProductType(product.productType);
			
			// 폼 필드 값 설정 (초기화 전에 reset)
			form.resetFields();
			
			// setTimeout을 사용하여 폼이 완전히 리셋된 후 값 설정
			setTimeout(() => {
				form.setFieldsValue({
					productName: product.productName || '',
					productType: product.productType || '',
					productSubType: product.productSubType === 'NONE' || !product.productSubType 
						? (subTypes.length === 0 ? 'NONE' : undefined)
						: product.productSubType,
					productPrice: parseInt(product.productPrice) || 0,
					productDescription: product.productDescription || '', // null/undefined 처리
					productImageUrl: product.productImageUrl || '',
					productActiveStatus: product.productActiveStatus || 'ACTIVE', // 상태 필드 추가
					options: product.options && product.options.length > 0
						? product.options.map(opt => ({
								optionNo: opt.optionNo,
								color: opt.color || '',
								size: opt.size || '',
								optionAddPrice: opt.optionAddPrice || 0,
							}))
						: [{ color: '', size: '', optionAddPrice: 0 }],
				});
			}, 0);
		} else if (!visible) {
			form.resetFields();
			setSelectedProductType(null);
			setImageItems([]);
		}
	}, [visible, product, form]);

	const handleProductTypeChange = (value) => {
		setSelectedProductType(value);
		const subTypes = PRODUCT_SUB_TYPES[value] || [];
		form.setFieldsValue({ 
			productSubType: subTypes.length === 0 ? 'NONE' : undefined 
		});
	};

	const handleSubmit = async () => {
		try {
			const values = await form.validateFields();
			// 이미지 목록 유효성
			if (!imageItems || imageItems.length === 0) {
				message.error('최소 1장의 상품 이미지를 업로드해주세요.');
				return;
			}
			const primary = imageItems.find(i => i.isPrimary) || imageItems[0];
			const ordered = [...imageItems].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
				.map((it, idx) => ({
					imageUrl: it.imageUrl,
					fileId: it.fileId || null,
					sortOrder: typeof it.sortOrder === 'number' ? it.sortOrder : idx,
					isPrimary: it.imageUrl === primary.imageUrl
				}));
			// 서버에 이미지 목록 먼저 반영
			await AdminProductService.updateImages(product.productNo, ordered);
			
			// 옵션 데이터 변환
			const options = values.options.map(opt => ({
				optionNo: opt.optionNo || null, // 기존 옵션 번호 (수정 시)
				color: opt.color || null,
				size: opt.size || null,
				optionAddPrice: opt.optionAddPrice || 0,
			}));

			// 요청 데이터 구성
			const productData = {
				productName: values.productName,
				productType: values.productType,
				productSubType: values.productSubType || 'NONE',
				productPrice: String(values.productPrice),
				productDescription: values.productDescription,
				// 대표 이미지 URL은 업로드 목록에서 선택된 대표로 동기화
				productImageUrl: primary?.imageUrl || values.productImageUrl,
				options: options,
			};

			await onSubmit(productData);
			form.resetFields();
			setSelectedProductType(null);
			setImageItems([]);
		} catch (error) {
			// Validation failed - handled by Ant Design Form
		}
	};

	const getAvailableSubTypes = () => {
		if (!selectedProductType) return [];
		return PRODUCT_SUB_TYPES[selectedProductType] || [];
	};

	if (!product) {
		return null;
	}

	const isRejected = product?.productActiveStatus === 'REJECTED';

	return (
		<Modal
			title={isRejected ? "상품 수정 및 재신청" : "상품 수정"}
			open={visible}
			onCancel={onCancel}
			onOk={handleSubmit}
			confirmLoading={loading}
			width={800}
			okText={isRejected ? "수정 및 재신청" : "수정"}
			cancelText="닫기"
		>
		{/* PENDING_UPDATE 상태에서는 이전 거절 사유를 숨김 (새로운 수정 요청이므로) */}
		{product?.rejectionReason && product.productActiveStatus !== 'PENDING_UPDATE' && (
			<Alert
				message={product.productActiveStatus === 'REJECTED' ? '거절 사유' : '수정 거절 사유'}
				description={product.rejectionReason}
				type={product.productActiveStatus === 'REJECTED' ? 'error' : 'warning'}
				icon={<ExclamationCircleOutlined />}
				showIcon
				style={{ marginBottom: 16 }}
			/>
		)}
		{product.productActiveStatus === 'PENDING_UPDATE' && (
			<Alert
				message="수정 승인 대기 중"
				description="재심사 필수 항목(상품명, 카테고리, 가격)을 수정하여 관리자 승인을 기다리는 중입니다."
				type="info"
				showIcon
				style={{ marginBottom: 16 }}
			/>
		)}
			{product.productActiveStatus === 'ACTIVE' && (
				<Alert
					message="상품 수정 안내"
					description="재심사 필수 항목(상품명, 카테고리, 가격)을 변경하면 관리자 승인이 필요합니다. 일반 항목(설명, 이미지)은 즉시 반영됩니다."
					type="info"
					showIcon
					style={{ marginBottom: 16 }}
				/>
			)}
			<Form
				form={form}
				layout="vertical"
			>
				<Alert
					type="info"
					message="상품 이미지"
					description="여러 장을 업로드하고 순서를 정할 수 있습니다. 대표 이미지를 선택하세요."
					showIcon
					style={{ marginBottom: 12 }}
				/>
				<Space direction="vertical" style={{ width: '100%', marginBottom: 8 }}>
					<Upload
						beforeUpload={(file) => {
							const ok = ['image/jpeg','image/png','image/gif','image/webp'].includes(file.type) && (file.size / 1024 / 1024) < 10;
							if (!ok) {
								message.error('이미지(jpg/png/gif/webp), 10MB 이하만 업로드 가능합니다.');
								return Upload.LIST_IGNORE;
							}
							return true;
						}}
						customRequest={async ({ file, onSuccess, onError }) => {
							try {
								const res = await FileService.uploadFile(file, 'product');
								const data = res?.data || res;
								const url = data?.fileUrl;
								const fileId = data?.fileId || null;
								if (!url) throw new Error('업로드 응답에 fileUrl이 없습니다.');
								setImageItems(prev => {
                                    const next = [...prev, {
                                        imageUrl: url,
                                        fileId,
                                        sortOrder: prev.length,
                                        isPrimary: prev.length === 0
                                    }];
                                    return next;
                                });
								onSuccess && onSuccess({}, file);
							} catch (e) {
								onError && onError(e);
								message.error(e?.response?.data?.message || e?.message || '이미지 업로드 실패');
							}
						}}
						showUploadList={false}
						multiple
					>
						<Button icon={<UploadOutlined />}>상품 이미지 업로드</Button>
					</Upload>
					<List
						dataSource={[...imageItems].sort((a,b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))}
						renderItem={(item, idx) => (
							<List.Item
								actions={[
									<Button size="small" onClick={() => {
										// 위로
										setImageItems(prev => {
											const arr = [...prev];
											const index = arr.findIndex(p => p.imageUrl === item.imageUrl);
											if (index > 0) {
												const tempOrder = arr[index - 1].sortOrder ?? (index - 1);
												arr[index - 1].sortOrder = (arr[index].sortOrder ?? index);
												arr[index].sortOrder = tempOrder;
											}
											return arr;
										});
									}}>↑</Button>,
									<Button size="small" onClick={() => {
										// 아래로
										setImageItems(prev => {
											const arr = [...prev];
											const index = arr.findIndex(p => p.imageUrl === item.imageUrl);
											if (index < arr.length - 1) {
												const tempOrder = arr[index + 1].sortOrder ?? (index + 1);
												arr[index + 1].sortOrder = (arr[index].sortOrder ?? index);
												arr[index].sortOrder = tempOrder;
											}
											return arr;
										});
									}}>↓</Button>,
									<Button danger size="small" onClick={() => {
										setImageItems(prev => prev
											.filter(p => p.imageUrl !== item.imageUrl)
											.map((p, i) => ({ ...p, sortOrder: i })));
									}}>삭제</Button>
								]}
							>
								<Space style={{ alignItems: 'center' }}>
									<img src={item.imageUrl} alt="" style={{ width: 64, height: 64, objectFit: 'cover', borderRadius: 4 }} />
									<Radio
										checked={!!item.isPrimary}
										onChange={() => {
											setImageItems(prev => prev.map(p => ({ ...p, isPrimary: p.imageUrl === item.imageUrl })));
											form.setFieldsValue({ productImageUrl: item.imageUrl });
										}}
									>
										대표
									</Radio>
									<span style={{ color: '#999' }}>순서: {item.sortOrder ?? idx}</span>
								</Space>
							</List.Item>
						)}
						locale={{ emptyText: '업로드된 이미지가 없습니다.' }}
						style={{ background: '#fafafa', padding: 8, borderRadius: 4 }}
					/>
				</Space>
				<Form.Item
					name="productName"
					label="상품명"
					rules={[{ required: true, message: '상품명을 입력해주세요.' }]}
				>
					<Input placeholder="상품명을 입력하세요" />
				</Form.Item>

				<Form.Item
					name="productType"
					label="상품 대분류"
					rules={[{ required: true, message: '상품 대분류를 선택해주세요.' }]}
				>
					<Select
						placeholder="대분류를 선택하세요"
						onChange={handleProductTypeChange}
					>
						{PRODUCT_TYPES.map((type) => (
							<Option key={type.value} value={type.value}>
								{type.label}
							</Option>
						))}
					</Select>
				</Form.Item>

				{getAvailableSubTypes().length > 0 && (
					<Form.Item
						name="productSubType"
						label="상품 소분류"
					>
						<Select placeholder="소분류를 선택하세요 (선택사항)">
							{getAvailableSubTypes().map((subType) => (
								<Option key={subType.value} value={subType.value}>
									{subType.label}
								</Option>
							))}
						</Select>
					</Form.Item>
				)}

				<Form.Item
					name="productPrice"
					label="기본 가격 (원)"
					rules={[
						{ required: true, message: '가격을 입력해주세요.' },
						{ type: 'number', min: 0, message: '가격은 0원 이상이어야 합니다.' },
					]}
				>
					<InputNumber
						style={{ width: '100%' }}
						placeholder="가격을 입력하세요"
						formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
						parser={(value) => value.replace(/\$\s?|(,*)/g, '')}
					/>
				</Form.Item>

				<Form.Item
					name="productDescription"
					label="상품 설명"
					rules={[{ required: true, message: '상품 설명을 입력해주세요.' }]}
				>
					<TextArea
						rows={4}
						placeholder="상품 설명을 입력하세요"
						maxLength={5000}
						showCount
					/>
				</Form.Item>

				<Form.Item name="productImageUrl" label="대표 이미지 URL">
					<Input placeholder="대표 이미지는 위 업로더에서 선택하세요" disabled />
				</Form.Item>

				<Form.Item label="옵션">
					<Form.List name="options">
						{(fields, { add, remove }) => (
							<>
								{fields.map(({ key, name, ...restField }) => (
									<Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
										<Form.Item
											{...restField}
											name={[name, 'optionNo']}
											hidden
										>
											<Input type="hidden" />
										</Form.Item>
										<Form.Item
											{...restField}
											name={[name, 'color']}
											style={{ width: 150 }}
										>
											<Input placeholder="색상 (예: 빨강)" />
										</Form.Item>
										<Form.Item
											{...restField}
											name={[name, 'size']}
											style={{ width: 150 }}
										>
											<Input placeholder="사이즈 (예: M)" />
										</Form.Item>
										<Form.Item
											{...restField}
											name={[name, 'optionAddPrice']}
											initialValue={0}
										>
											<InputNumber
												placeholder="추가 가격"
												formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
												parser={(value) => value.replace(/\$\s?|(,*)/g, '')}
												style={{ width: 150 }}
											/>
										</Form.Item>
										{fields.length > 1 && (
											<MinusCircleOutlined onClick={() => remove(name)} />
										)}
									</Space>
								))}
								<Form.Item>
									<Button
										type="dashed"
										onClick={() => add()}
										block
										icon={<PlusOutlined />}
									>
										옵션 추가
									</Button>
								</Form.Item>
							</>
						)}
					</Form.List>
				</Form.Item>
			</Form>
		</Modal>
	);
};

export default ProductEditModal;
