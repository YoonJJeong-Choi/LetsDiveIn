import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Select, Button, Space, message, Card, Upload } from 'antd';
import { PlusOutlined, MinusCircleOutlined, UploadOutlined, DeleteOutlined } from '@ant-design/icons';
import AdminService from 'services/AdminService';
import FileService from 'services/FileService';
import { resolveMediaUrl } from 'utils/resolveMediaUrl';

const { TextArea } = Input;
const { Option } = Select;

// 상품 대분류 옵션
const PRODUCT_TYPES = [
	{ value: 'SWIMSUIT_MEN', label: '남성 수영복' },
	{ value: 'SWIMSUIT_WOMEN', label: '여성 수영복' },
	{ value: 'SWIMSUIT_KIDS', label: '아동 수영복' },
	{ value: 'SWIM_CAP', label: '수모' },
	{ value: 'SWIM_GOGGLES', label: '수경' },
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

const SWIMSUIT_SIZE_OPTIONS = ['XS', 'S', 'M', 'L', 'XL', 'XXL'];
const FINS_SIZE_OPTIONS = ['220', '225', '230', '235', '240', '245', '250', '255', '260', '265', '270', '275', '280', '285', '290', '295', '300'];
const SIZE_TEMPLATES = {
	SWIMSUIT_MEN_PANTS: ['waistCm', 'hipCm'],
	SWIMSUIT_WOMEN_ONEPIECE: ['chestCm', 'waistCm', 'hipCm', 'torsoCm'],
	SWIMSUIT_WOMEN_BIKINI: ['chestCm', 'waistCm', 'hipCm'],
	FINS_SIZE: ['footLengthCm'],
};
const TEMPLATE_LABELS = {
	waistCm: '허리',
	hipCm: '엉덩이',
	chestCm: '가슴',
	torsoCm: '몸통',
	footLengthCm: '발길이(cm)',
};

const isStructuredSizeType = (productType) => ['SWIMSUIT_MEN', 'SWIMSUIT_WOMEN', 'FINS'].includes(productType);
const getSizeChoices = (productType) => {
	if (productType === 'FINS') return FINS_SIZE_OPTIONS;
	if (productType === 'SWIMSUIT_MEN' || productType === 'SWIMSUIT_WOMEN') return SWIMSUIT_SIZE_OPTIONS;
	return [];
};
const composeOptionSize = (opt, productType) => {
	if (!isStructuredSizeType(productType)) return opt.size || null;
	const label = opt.sizeLabel || null;
	if (!label) return null;
	return label;
};
const isSwimsuitType = (productType) => ['SWIMSUIT_MEN', 'SWIMSUIT_WOMEN'].includes(productType);
const isFinsType = (productType) => productType === 'FINS';
const getSizeGuideTemplateKey = (productType, productSubType) => {
	if (productType === 'FINS') return 'FINS_SIZE';
	if (productType === 'SWIMSUIT_MEN') return 'SWIMSUIT_MEN_PANTS';
	if (productType === 'SWIMSUIT_WOMEN') {
		return productSubType === 'BIKINI' ? 'SWIMSUIT_WOMEN_BIKINI' : 'SWIMSUIT_WOMEN_ONEPIECE';
	}
	return null;
};

const beforeUploadImage = (file) => {
	const isAllowed = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type);
	if (!isAllowed) {
		message.error('이미지 파일만 업로드할 수 있습니다 (jpg, png, gif, webp).');
		return Upload.LIST_IGNORE;
	}
	const isLt10M = file.size / 1024 / 1024 < 10;
	if (!isLt10M) {
		message.error('파일 용량은 10MB 이하만 허용됩니다.');
		return Upload.LIST_IGNORE;
	}
	return true;
};

const ProductCreateModal = ({ visible, onCancel, onSubmit, loading, embedded = false }) => {
	const [form] = Form.useForm();
	const [selectedProductType, setSelectedProductType] = useState(null);
	const selectedProductSubType = Form.useWatch('productSubType', form);
	const productImageUrl = Form.useWatch('productImageUrl', form);
	const [colorOptions, setColorOptions] = useState([]);
	const [uploadingImage, setUploadingImage] = useState(false);

	useEffect(() => {
		if (!visible) {
			form.resetFields();
			setSelectedProductType(null);
		}
	}, [visible, form]);

	useEffect(() => {
		const fetchColorOptions = async () => {
			if (!visible) return;
			try {
				const response = await AdminService.getActiveColors();
				const rows = response?.data ?? response ?? [];
				setColorOptions(
					(Array.isArray(rows) ? rows : []).map((row) => ({
						value: row.code,
						label: row.label || row.code,
					}))
				);
			} catch (err) {
				setColorOptions([]);
				message.error(err?.response?.data?.message || err?.message || '컬러 목록 조회에 실패했습니다.');
			}
		};
		fetchColorOptions();
	}, [visible]);

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
				size: composeOptionSize(opt, values.productType),
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
				materialInfo: values.materialInfo || null,
				originCountry: values.originCountry || null,
				manufactureCountry: values.manufactureCountry || null,
				careInstructions: values.careInstructions || null,
				sizeGuideText: (() => {
					if (isSwimsuitType(values.productType) || isFinsType(values.productType)) {
						return null;
					}
					return values.sizeGuideText || null;
				})(),
				sizeGuideJson: (() => {
					const templateKey = getSizeGuideTemplateKey(values.productType, values.productSubType || 'NONE');
					if (!templateKey) return null;
					const metrics = SIZE_TEMPLATES[templateKey] || [];
					const rows = (values.sizeGuideRows || [])
						.filter((r) => r?.sizeLabel)
						.map((r) => {
							const row = { sizeLabel: r.sizeLabel };
							metrics.forEach((metric) => {
								row[metric] = r[metric] ?? null;
							});
							return row;
						});
					return rows.length > 0 ? JSON.stringify({ templateKey, rows }) : null;
				})(),
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
	const colorSelectOptions = colorOptions;

	const handleUploadProductImage = async ({ file, onSuccess, onError }) => {
		try {
			setUploadingImage(true);
			const res = await FileService.uploadFile(file, 'product');
			const data = res?.data || res;
			const fileUrl = data?.fileUrl;
			if (!fileUrl) {
				throw new Error('업로드 응답에 fileUrl이 없습니다.');
			}
			form.setFieldsValue({ productImageUrl: fileUrl });
			message.success('상품 이미지가 업로드되었습니다.');
			onSuccess && onSuccess({}, file);
		} catch (error) {
			onError && onError(error);
			message.error(error?.response?.data?.message || error?.message || '상품 이미지 업로드에 실패했습니다.');
		} finally {
			setUploadingImage(false);
		}
	};

	const formContent = (
			<Form
				form={form}
				layout="vertical"
				initialValues={{
					options: [{ color: undefined, size: '', sizeLabel: '', optionAddPrice: 0 }],
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

				<Form.Item name="materialInfo" label="소재/혼용률">
					<TextArea rows={2} placeholder="예: 폴리에스터 82%, 스판덱스 18%" maxLength={1000} showCount />
				</Form.Item>

				<Form.Item name="originCountry" label="원산지">
					<Input placeholder="예: 대한민국" />
				</Form.Item>

				<Form.Item name="manufactureCountry" label="제조국">
					<Input placeholder="예: 대한민국" />
				</Form.Item>

				<Form.Item name="careInstructions" label="세탁 권장 사항">
					<TextArea rows={2} placeholder="예: 세탁기 사용 시 30°C 이하 권장 / 단시간 탈수 권장" maxLength={1000} showCount />
				</Form.Item>

				{(isSwimsuitType(selectedProductType) || isFinsType(selectedProductType)) ? (
					<Form.List name="sizeGuideRows">
						{(fields, { add, remove }) => (
							<Form.Item label={isFinsType(selectedProductType) ? "사이즈표(오리발)" : "사이즈표(수영복)"}>
								{fields.map(({ key, name, ...restField }) => (
									<Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
										<Form.Item {...restField} name={[name, 'sizeLabel']} rules={[{ required: true, message: '사이즈' }]}>
											<Select placeholder={isFinsType(selectedProductType) ? "신발(mm)" : "사이즈"} style={{ width: 120 }}>
												{(isFinsType(selectedProductType) ? FINS_SIZE_OPTIONS : SWIMSUIT_SIZE_OPTIONS).map((s) => (
													<Option key={s} value={s}>{isFinsType(selectedProductType) ? `${s}mm` : s}</Option>
												))}
											</Select>
										</Form.Item>
										{(SIZE_TEMPLATES[getSizeGuideTemplateKey(selectedProductType, selectedProductSubType || 'NONE')] || []).map((metric) => (
											<Form.Item key={metric} {...restField} name={[name, metric]}>
												<InputNumber placeholder={TEMPLATE_LABELS[metric]} min={1} style={{ width: 110 }} />
											</Form.Item>
										))}
										{fields.length > 1 && <MinusCircleOutlined onClick={() => remove(name)} />}
									</Space>
								))}
								<Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>사이즈 행 추가</Button>
							</Form.Item>
						)}
					</Form.List>
				) : (
					<Form.Item name="sizeGuideText" label="사이즈표(텍스트)">
						<TextArea rows={4} placeholder={"예: Free: 가슴 90~100 / 허리 70~80"} maxLength={5000} showCount />
					</Form.Item>
				)}

				<Form.Item
					name="productImageUrl"
					label="상품 이미지"
					rules={[{ required: true, message: '상품 이미지를 업로드해주세요.' }]}
				>
					<Input type="hidden" />
				</Form.Item>
				<Form.Item label="상품 이미지 업로드" required>
					<Space align="start" wrap>
						{productImageUrl && (
							<img
								src={resolveMediaUrl(productImageUrl)}
								alt="상품 이미지 미리보기"
								style={{ width: 96, height: 96, objectFit: 'cover', borderRadius: 6, border: '1px solid #f0f0f0' }}
							/>
						)}
						<Space direction="vertical">
							<Upload
								maxCount={1}
								beforeUpload={beforeUploadImage}
								customRequest={handleUploadProductImage}
								showUploadList={false}
							>
								<Button icon={<UploadOutlined />} loading={uploadingImage}>
									이미지 업로드
								</Button>
							</Upload>
							{productImageUrl && (
								<Button
									icon={<DeleteOutlined />}
									onClick={() => form.setFieldsValue({ productImageUrl: '' })}
								>
									이미지 제거
								</Button>
							)}
							<span className="text-muted">jpg, png, gif, webp / 10MB 이하</span>
						</Space>
					</Space>
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
											rules={[{ required: true, message: '컬러 선택' }]}
										>
											<Select
												placeholder="컬러 선택"
												options={colorSelectOptions}
												showSearch
												optionFilterProp="label"
											/>
										</Form.Item>
										{isStructuredSizeType(selectedProductType) ? (
											<>
												<Form.Item
													{...restField}
													name={[name, 'sizeLabel']}
													style={{ width: 140 }}
													rules={[{ required: true, message: '사이즈 선택' }]}
												>
													<Select placeholder="사이즈">
														{getSizeChoices(selectedProductType).map((size) => (
															<Option key={size} value={size}>
																{selectedProductType === 'FINS' ? `${size}mm` : size}
															</Option>
														))}
													</Select>
												</Form.Item>
											</>
										) : (
											<Form.Item
												{...restField}
												name={[name, 'size']}
												style={{ width: 150 }}
											>
												<Input placeholder="사이즈 (예: M)" />
											</Form.Item>
										)}
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
	);

	if (embedded) {
		return (
			<Card>
				{formContent}
				<div className="text-right">
					<Button className="mr-2" onClick={onCancel}>취소</Button>
					<Button type="primary" loading={loading} onClick={handleSubmit}>신청</Button>
				</div>
			</Card>
		);
	}

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
			{formContent}
		</Modal>
	);
};

export default ProductCreateModal;
