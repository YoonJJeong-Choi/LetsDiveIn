import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Select, Button, Space, message } from 'antd';
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';

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

const ProductCreateModal = ({ visible, onCancel, onSubmit, loading }) => {
	const [form] = Form.useForm();
	const [selectedProductType, setSelectedProductType] = useState(null);

	useEffect(() => {
		if (!visible) {
			form.resetFields();
			setSelectedProductType(null);
		}
	}, [visible, form]);

	const handleProductTypeChange = (value) => {
		setSelectedProductType(value);
		// 소분류가 없는 대분류면 NONE으로 설정, 있으면 undefined
		const subTypes = PRODUCT_SUB_TYPES[value] || [];
		form.setFieldsValue({ 
			productSubType: subTypes.length === 0 ? 'NONE' : undefined 
		});
	};

	const handleSubmit = async () => {
		try {
			const values = await form.validateFields();
			
			// 옵션 데이터 변환
			const options = values.options.map(opt => ({
				color: opt.color || null,
				size: opt.size || null,
				optionAddPrice: opt.optionAddPrice || 0,
			}));

			// 요청 데이터 구성
			// 소분류가 없으면 NONE으로 설정 (null 대신 명시적으로 처리)
			const productSubType = values.productSubType || 'NONE';
			
			const productData = {
				productName: values.productName,
				productType: values.productType,
				productSubType: productSubType, // 항상 값이 있음 (NONE 또는 선택한 값)
				productPrice: String(values.productPrice),
				productDescription: values.productDescription,
				productImageUrl: values.productImageUrl,
				options: options,
			};

			await onSubmit(productData);
			form.resetFields();
			setSelectedProductType(null);
		} catch (error) {
			// Validation failed - handled by Ant Design Form
		}
	};

	const getAvailableSubTypes = () => {
		if (!selectedProductType) return [];
		return PRODUCT_SUB_TYPES[selectedProductType] || [];
	};

	return (
		<Modal
			title="상품 등록 신청"
			open={visible}
			onCancel={onCancel}
			onOk={handleSubmit}
			confirmLoading={loading}
			width={800}
			okText="신청"
			cancelText="취소"
		>
			<Form
				form={form}
				layout="vertical"
				initialValues={{
					options: [{ color: '', size: '', optionAddPrice: 0 }],
				}}
			>
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

				{getAvailableSubTypes().length > 0 ? (
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
				) : (
					// 소분류가 없는 대분류의 경우 자동으로 NONE 설정 (사용자에게는 표시 안 함)
					<Form.Item name="productSubType" hidden initialValue="NONE">
						<Input />
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

				<Form.Item
					name="productImageUrl"
					label="상품 이미지 URL"
					rules={[{ required: true, message: '이미지 URL을 입력해주세요.' }]}
				>
					<Input placeholder="/images/products/..." />
				</Form.Item>

				<Form.Item label="옵션">
					<Form.List name="options">
						{(fields, { add, remove }) => (
							<>
								{fields.map(({ key, name, ...restField }) => (
									<Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
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

export default ProductCreateModal;
