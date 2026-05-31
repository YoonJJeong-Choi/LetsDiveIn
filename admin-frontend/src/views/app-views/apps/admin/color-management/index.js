import React, { useEffect, useMemo, useState } from 'react';
import { Card, Table, Button, Space, Tag, Modal, Form, Input, InputNumber, Switch, message, Popconfirm } from 'antd';
import { PlusOutlined, ReloadOutlined, DeleteOutlined } from '@ant-design/icons';
import AdminService from 'services/AdminService';

const ColorManagement = () => {
	const [loading, setLoading] = useState(false);
	const [colors, setColors] = useState([]);
	const [colorModalVisible, setColorModalVisible] = useState(false);
	const [synModalVisible, setSynModalVisible] = useState(false);
	const [selectedCode, setSelectedCode] = useState(null);
	const [colorForm] = Form.useForm();
	const [synForm] = Form.useForm();

	const fetchColors = async () => {
		try {
			setLoading(true);
			const response = await AdminService.getAdminColors();
			const data = response?.data ?? response ?? [];
			setColors(Array.isArray(data) ? data : []);
		} catch (err) {
			message.error(err?.response?.data?.message || err?.message || '컬러 목록 조회에 실패했습니다.');
			setColors([]);
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		fetchColors();
	}, []);

	const openCreateModal = () => {
		colorForm.resetFields();
		colorForm.setFieldsValue({ isActive: true, sortOrder: 100, hex: null });
		setColorModalVisible(true);
	};

	const handleCreateColor = async () => {
		try {
			const values = await colorForm.validateFields();
			await AdminService.createAdminColor({
				code: values.code?.trim(),
				label: values.label?.trim(),
				hex: values.hex ? values.hex.trim() : null,
				sortOrder: values.sortOrder,
				isActive: !!values.isActive,
			});
			message.success('컬러가 저장되었습니다.');
			setColorModalVisible(false);
			await fetchColors();
		} catch (err) {
			if (err?.errorFields) return;
			message.error(err?.response?.data?.message || err?.message || '컬러 저장에 실패했습니다.');
		}
	};

	const handleToggleStatus = async (code, isActive) => {
		try {
			await AdminService.updateAdminColorStatus(code, isActive);
			message.success(`컬러를 ${isActive ? '활성화' : '비활성화'}했습니다.`);
			await fetchColors();
		} catch (err) {
			message.error(err?.response?.data?.message || err?.message || '상태 변경에 실패했습니다.');
		}
	};

	const openSynonymModal = (code) => {
		setSelectedCode(code);
		synForm.resetFields();
		setSynModalVisible(true);
	};

	const handleAddSynonym = async () => {
		try {
			const values = await synForm.validateFields();
			await AdminService.addAdminColorSynonym(selectedCode, values.synonym?.trim());
			message.success('유사어가 추가되었습니다.');
			setSynModalVisible(false);
			setSelectedCode(null);
			await fetchColors();
		} catch (err) {
			if (err?.errorFields) return;
			message.error(err?.response?.data?.message || err?.message || '유사어 추가에 실패했습니다.');
		}
	};

	const handleDeleteSynonym = async (synonymId) => {
		try {
			await AdminService.deleteAdminColorSynonym(synonymId);
			message.success('유사어를 삭제했습니다.');
			await fetchColors();
		} catch (err) {
			message.error(err?.response?.data?.message || err?.message || '유사어 삭제에 실패했습니다.');
		}
	};

	const colorRows = useMemo(
		() => colors.map((c, index) => ({ key: `${c.code}-${index}`, ...c })),
		[colors]
	);

	const columns = [
		{
			title: '코드',
			dataIndex: 'code',
			key: 'code',
			width: 140,
			render: (code) => <Tag color="blue">{code}</Tag>,
		},
		{
			title: '라벨',
			dataIndex: 'label',
			key: 'label',
			width: 160,
		},
		{
			title: '활성',
			dataIndex: 'isActive',
			key: 'isActive',
			width: 100,
			render: (isActive, record) => (
				<Switch checked={!!isActive} onChange={(checked) => handleToggleStatus(record.code, checked)} />
			),
		},
		{
			title: '정렬',
			dataIndex: 'sortOrder',
			key: 'sortOrder',
			width: 100,
		},
		{
			title: '유사어',
			dataIndex: 'synonyms',
			key: 'synonyms',
			render: (synonyms = [], record) => (
				<Space size={[8, 8]} wrap>
					{(synonyms || []).map((syn) => {
						const synId = typeof syn === 'object' ? syn.id : null;
						const synText = typeof syn === 'object' ? syn.synonym : String(syn);
						return (
							<Space key={`${record.code}-${synId || synText}`} size={4}>
								<Tag>{synText}</Tag>
								{synId ? (
									<Popconfirm
										title="유사어 삭제"
										description="이 유사어를 삭제하시겠습니까?"
										onConfirm={() => handleDeleteSynonym(synId)}
										okText="삭제"
										cancelText="취소"
									>
										<Button type="text" danger size="small" icon={<DeleteOutlined />} />
									</Popconfirm>
								) : null}
							</Space>
						);
					})}
					<Button size="small" type="dashed" onClick={() => openSynonymModal(record.code)}>
						유사어 추가
					</Button>
				</Space>
			),
		},
	];

	return (
		<>
			<Card
				title="컬러 관리"
				extra={
					<Space>
						<Button icon={<ReloadOutlined />} onClick={fetchColors} loading={loading}>
							새로고침
						</Button>
						<Button type="primary" icon={<PlusOutlined />} onClick={openCreateModal}>
							컬러 추가
						</Button>
					</Space>
				}
			>
				<Table
					columns={columns}
					dataSource={colorRows}
					loading={loading}
					pagination={{ pageSize: 20, showSizeChanger: true }}
					size="small"
				/>
			</Card>

			<Modal
				title="컬러 추가/업데이트"
				open={colorModalVisible}
				onOk={handleCreateColor}
				onCancel={() => setColorModalVisible(false)}
				okText="저장"
				cancelText="취소"
			>
				<Form form={colorForm} layout="vertical">
					<Form.Item
						name="code"
						label="컬러 코드"
						rules={[
							{ required: true, message: '코드를 입력해주세요.' },
							{ pattern: /^[A-Z0-9_]+$/, message: '영문 대문자/숫자/_만 가능합니다.' },
						]}
					>
						<Input placeholder="예: BLACK" />
					</Form.Item>
					<Form.Item
						name="label"
						label="표시 라벨"
						rules={[{ required: true, message: '라벨을 입력해주세요.' }]}
					>
						<Input placeholder="예: 블랙" />
					</Form.Item>
					<Form.Item name="hex" label="HEX (선택)">
						<Input placeholder="예: #000000" />
					</Form.Item>
					<Form.Item
						name="sortOrder"
						label="정렬 순서"
						rules={[{ required: true, message: '정렬 순서를 입력해주세요.' }]}
					>
						<InputNumber style={{ width: '100%' }} min={0} />
					</Form.Item>
					<Form.Item name="isActive" label="활성 여부" valuePropName="checked">
						<Switch />
					</Form.Item>
				</Form>
			</Modal>

			<Modal
				title={`유사어 추가 (${selectedCode || '-'})`}
				open={synModalVisible}
				onOk={handleAddSynonym}
				onCancel={() => {
					setSynModalVisible(false);
					setSelectedCode(null);
				}}
				okText="추가"
				cancelText="취소"
			>
				<Form form={synForm} layout="vertical">
					<Form.Item
						name="synonym"
						label="유사어"
						rules={[{ required: true, message: '유사어를 입력해주세요.' }]}
					>
						<Input placeholder="예: 검정, black" />
					</Form.Item>
				</Form>
			</Modal>
		</>
	);
};

export default ColorManagement;
