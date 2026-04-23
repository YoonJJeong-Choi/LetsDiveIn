import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Modal, Tag, Space, Row, Col, Statistic, Descriptions, Spin, Select, Input, InputNumber, message, Tabs, Form, Popconfirm, Timeline, Empty, Checkbox, DatePicker } from 'antd';
import { UserOutlined, EyeOutlined, ReloadOutlined, SearchOutlined, MailOutlined, ShoppingOutlined, StarOutlined, UndoOutlined, EditOutlined, LockOutlined, UnlockOutlined, KeyOutlined, SendOutlined, HistoryOutlined, FileTextOutlined, PlusOutlined, DeleteOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import AdminService from 'services/AdminService';
import dayjs from 'dayjs';

const { Option } = Select;
const { TabPane } = Tabs;

const CustomerManagement = () => {
	const [customers, setCustomers] = useState([]);
	const [loading, setLoading] = useState(false);
	const [statisticsLoading, setStatisticsLoading] = useState(false);
	const [detailModalVisible, setDetailModalVisible] = useState(false);
	const [selectedCustomer, setSelectedCustomer] = useState(null);
	const [customerDetail, setCustomerDetail] = useState(null);
	const [customerDetailLoading, setCustomerDetailLoading] = useState(false);
	const [statistics, setStatistics] = useState(null);
	const [customerHistory, setCustomerHistory] = useState([]);
	const [historyLoading, setHistoryLoading] = useState(false);
	const [customerNotes, setCustomerNotes] = useState([]);
	const [notesLoading, setNotesLoading] = useState(false);
	const [noteModalVisible, setNoteModalVisible] = useState(false);
	const [editingNote, setEditingNote] = useState(null);
	const [noteForm] = Form.useForm();
	const [allTags, setAllTags] = useState([]);
	const [tagsLoading, setTagsLoading] = useState(false);
	const [tagModalVisible, setTagModalVisible] = useState(false);
	const [editingTag, setEditingTag] = useState(null);
	const [tagForm] = Form.useForm();
	const [addTagModalVisible, setAddTagModalVisible] = useState(false);
	const [activityLogs, setActivityLogs] = useState([]);
	const [activityLogsLoading, setActivityLogsLoading] = useState(false);
	
	// 필터 및 페이징 상태
	const [page, setPage] = useState(0);
	const [pageSize, setPageSize] = useState(20);
	const [totalCount, setTotalCount] = useState(0);
	const [searchKeyword, setSearchKeyword] = useState('');
	const [emailVerifiedFilter, setEmailVerifiedFilter] = useState(null);
	const [gradeFilter, setGradeFilter] = useState(null);
	const [detailTab, setDetailTab] = useState('info');

	// 포인트 탭 상태 (서버 페이지네이션)
	const [pointHistory, setPointHistory] = useState([]);
	const [pointLoading, setPointLoading] = useState(false);
	const [pointPage, setPointPage] = useState(1);
	const [pointPageSize, setPointPageSize] = useState(10);
	const [pointTotal, setPointTotal] = useState(0);
	const [pointModalVisible, setPointModalVisible] = useState(false);
	const [pointModalMode, setPointModalMode] = useState('add'); // 'add' | 'deduct'
	const [pointForm] = Form.useForm();
	
	// 수정 모달 상태
	const [editModalVisible, setEditModalVisible] = useState(false);
	const [editForm] = Form.useForm();
	const [updating, setUpdating] = useState(false);

	useEffect(() => {
		fetchCustomerList();
		fetchStatistics();
		fetchAllTags();
	}, [page, pageSize, searchKeyword, emailVerifiedFilter, gradeFilter]);

	const fetchCustomerList = async () => {
		try {
			setLoading(true);
			const response = await AdminService.getCustomerList({
				page,
				pageSize,
				searchKeyword: searchKeyword || undefined,
				emailVerified: emailVerifiedFilter,
				grade: gradeFilter || undefined,
			});
			const data = response.data || response;
			// 표준(meta) 또는 기존 키(totalCount/page/pageSize) 모두 호환
			const meta = data?.meta || {
				page: typeof data?.page === 'number' ? data.page : page,
				size: typeof data?.size === 'number' ? data.size : (typeof data?.pageSize === 'number' ? data.pageSize : pageSize),
				total: typeof data?.total === 'number' ? data.total : (typeof data?.totalCount === 'number' ? data.totalCount : 0),
			};
			console.log('[Admin Customers] response meta =>', meta);
			setCustomers(data.customers || data.items || []);
			setTotalCount(Number(meta.total) || 0);
		} catch (err) {
			console.error('고객 목록 조회 실패:', err);
			message.error(err.response?.data?.message || '고객 목록을 불러오는데 실패했습니다.');
			setCustomers([]);
		} finally {
			setLoading(false);
		}
	};

	const fetchStatistics = async () => {
		try {
			setStatisticsLoading(true);
			const response = await AdminService.getCustomerStatistics();
			const data = response.data || response;
			setStatistics(data);
		} catch (err) {
			console.error('고객 통계 조회 실패:', err);
		} finally {
			setStatisticsLoading(false);
		}
	};

	const handleViewDetail = async (customerId) => {
		try {
			setCustomerDetailLoading(true);
			const response = await AdminService.getCustomerDetail(customerId);
			const data = response.data || response;
			setSelectedCustomer(customers.find(c => c.customerId === customerId));
			setCustomerDetail(data);
			setDetailModalVisible(true);
			setDetailTab('info');
			// 작업 이력, 메모, 태그, 활동 로그, 포인트 내역도 함께 조회
			await Promise.all([
				fetchCustomerHistory(customerId),
				fetchCustomerNotes(customerId),
				fetchCustomerTags(customerId),
				fetchCustomerActivityLogs(customerId),
				fetchCustomerPointHistory(customerId)
			]);
		} catch (err) {
			console.error('고객 상세 조회 실패:', err);
			message.error(err.response?.data?.message || '고객 상세 정보를 불러오는데 실패했습니다.');
		} finally {
			setCustomerDetailLoading(false);
		}
	};

	const fetchCustomerNotes = async (customerId) => {
		try {
			setNotesLoading(true);
			const response = await AdminService.getCustomerNotes(customerId);
			const data = response.data || response;
			setCustomerNotes(data || []);
		} catch (err) {
			console.error('고객 메모 조회 실패:', err);
			setCustomerNotes([]);
		} finally {
			setNotesLoading(false);
		}
	};

	const fetchCustomerPointHistory = async (customerId) => {
		try {
			setPointLoading(true);
			console.log('[Admin PointHistory] request params =>', { customerId, page: pointPage, size: pointPageSize });
			const response = await AdminService.getCustomerPointHistory(customerId, { page: pointPage, size: pointPageSize });
			const data = response.data || response;
			const meta = data?.meta;
			const items = data?.pointHistory ?? data?.items ?? data ?? [];
			console.log('[Admin PointHistory] response meta =>', { page: meta?.page ?? data?.page, size: meta?.size ?? data?.size, total: meta?.total ?? data?.total, itemsCount: Array.isArray(items) ? items.length : 0 });
			setPointHistory(Array.isArray(items) ? items : []);
			const total = (meta?.total ?? data?.total ?? (Array.isArray(items) ? items.length : 0));
			setPointTotal(Number(total) || 0);
		} catch (err) {
			console.error('고객 포인트 내역 조회 실패:', err);
			message.error(err.response?.data?.message || '포인트 내역을 불러오는데 실패했습니다.');
			setPointHistory([]);
			setPointTotal(0);
		} finally {
			setPointLoading(false);
		}
	};

	const handleOpenPointModal = (mode) => {
		setPointModalMode(mode);
		pointForm.resetFields();
		setPointModalVisible(true);
	};

	const handleSubmitPoint = async () => {
		try {
			if (!customerDetail || !customerDetail.customerId) {
				message.error('고객 정보를 찾을 수 없습니다.');
				return;
			}
			
			const values = await pointForm.validateFields();
			const payload = {
				pointAmount: values.pointAmount,
				description: values.description,
			};
			
			if (pointModalMode === 'add') {
				await AdminService.addCustomerPoint(customerDetail.customerId, payload);
				message.success('포인트가 지급되었습니다.');
			} else {
				await AdminService.deductCustomerPoint(customerDetail.customerId, payload);
				message.success('포인트가 차감되었습니다.');
			}
			
			setPointModalVisible(false);
			pointForm.resetFields();
			
			// 포인트 내역 새로고침
			await fetchCustomerPointHistory(customerDetail.customerId);
			
			// 고객 상세 정보도 새로고침 (포인트 잔액 업데이트를 위해)
			const detailResponse = await AdminService.getCustomerDetail(customerDetail.customerId);
			const detailData = detailResponse.data || detailResponse;
			setCustomerDetail(detailData);
		} catch (err) {
			if (err?.errorFields) {
				// 폼 검증 에러
				return;
			}
			console.error('포인트 수동 처리 실패:', err);
			message.error(err.response?.data?.message || '포인트 처리에 실패했습니다.');
		}
	};

	const handleOpenNoteModal = (note = null) => {
		setEditingNote(note);
		if (note) {
			noteForm.setFieldsValue({
				noteContent: note.noteContent,
				isImportant: note.isImportant
			});
		} else {
			noteForm.resetFields();
		}
		setNoteModalVisible(true);
	};

	const handleSaveNote = async () => {
		try {
			await noteForm.validateFields();
			const values = noteForm.getFieldsValue();
			
			if (editingNote) {
				await AdminService.updateCustomerNote(customerDetail.customerId, editingNote.noteId, values);
				message.success('메모가 수정되었습니다.');
			} else {
				await AdminService.createCustomerNote(customerDetail.customerId, values);
				message.success('메모가 작성되었습니다.');
			}
			
			setNoteModalVisible(false);
			setEditingNote(null);
			noteForm.resetFields();
			await fetchCustomerNotes(customerDetail.customerId);
		} catch (err) {
			console.error('메모 저장 실패:', err);
			message.error(err.response?.data?.message || '메모 저장에 실패했습니다.');
		}
	};

	const handleDeleteNote = async (noteId) => {
		try {
			await AdminService.deleteCustomerNote(customerDetail.customerId, noteId);
			message.success('메모가 삭제되었습니다.');
			await fetchCustomerNotes(customerDetail.customerId);
		} catch (err) {
			console.error('메모 삭제 실패:', err);
			message.error(err.response?.data?.message || '메모 삭제에 실패했습니다.');
		}
	};

	const fetchCustomerTags = async (customerId) => {
		try {
			const response = await AdminService.getCustomerTags(customerId);
			const data = response.data || response;
			// 고객 상세 정보에 태그 업데이트
			if (customerDetail) {
				setCustomerDetail({ ...customerDetail, tags: data || [] });
			}
		} catch (err) {
			console.error('고객 태그 조회 실패:', err);
		}
	};

	const fetchAllTags = async () => {
		try {
			setTagsLoading(true);
			const response = await AdminService.getAllCustomerTags();
			const data = response.data || response;
			setAllTags(data || []);
		} catch (err) {
			console.error('태그 목록 조회 실패:', err);
			setAllTags([]);
		} finally {
			setTagsLoading(false);
		}
	};

	const handleOpenTagModal = (tag = null) => {
		setEditingTag(tag);
		if (tag) {
			tagForm.setFieldsValue({
				tagName: tag.tagName,
				tagColor: tag.tagColor,
				description: tag.description
			});
		} else {
			tagForm.resetFields();
			tagForm.setFieldsValue({ tagColor: '#1890ff' });
		}
		setTagModalVisible(true);
	};

	const handleSaveTag = async () => {
		try {
			await tagForm.validateFields();
			const values = tagForm.getFieldsValue();
			
			// 색상 값 정규화 (이미 hex 문자열이므로 그대로 사용)
			
			if (editingTag) {
				await AdminService.updateCustomerTag(editingTag.tagId, values);
				message.success('태그가 수정되었습니다.');
			} else {
				await AdminService.createCustomerTag(values);
				message.success('태그가 생성되었습니다.');
			}
			
			setTagModalVisible(false);
			setEditingTag(null);
			tagForm.resetFields();
			await fetchAllTags();
		} catch (err) {
			console.error('태그 저장 실패:', err);
			message.error(err.response?.data?.message || '태그 저장에 실패했습니다.');
		}
	};

	const handleDeleteTag = async (tagId) => {
		try {
			await AdminService.deleteCustomerTag(tagId);
			message.success('태그가 삭제되었습니다.');
			await fetchAllTags();
		} catch (err) {
			console.error('태그 삭제 실패:', err);
			message.error(err.response?.data?.message || '태그 삭제에 실패했습니다.');
		}
	};

	const handleAddTagToCustomer = async (tagId) => {
		try {
			await AdminService.addTagToCustomer(customerDetail.customerId, tagId);
			message.success('태그가 추가되었습니다.');
			setAddTagModalVisible(false);
			await fetchCustomerTags(customerDetail.customerId);
			// 고객 상세 정보 새로고침
			await handleViewDetail(customerDetail.customerId);
		} catch (err) {
			console.error('태그 추가 실패:', err);
			message.error(err.response?.data?.message || '태그 추가에 실패했습니다.');
		}
	};

	const handleRemoveTagFromCustomer = async (tagId) => {
		try {
			await AdminService.removeTagFromCustomer(customerDetail.customerId, tagId);
			message.success('태그가 제거되었습니다.');
			await fetchCustomerTags(customerDetail.customerId);
			// 고객 상세 정보 새로고침
			await handleViewDetail(customerDetail.customerId);
		} catch (err) {
			console.error('태그 제거 실패:', err);
			message.error(err.response?.data?.message || '태그 제거에 실패했습니다.');
		}
	};

	const fetchCustomerActivityLogs = async (customerId) => {
		try {
			setActivityLogsLoading(true);
			const response = await AdminService.getCustomerActivityLogs(customerId, 0, 50);
			const data = response.data || response;
			setActivityLogs(data || []);
		} catch (err) {
			console.error('활동 로그 조회 실패:', err);
			setActivityLogs([]);
		} finally {
			setActivityLogsLoading(false);
		}
	};

	const fetchCustomerHistory = async (customerId) => {
		try {
			setHistoryLoading(true);
			const response = await AdminService.getCustomerHistory(customerId);
			const data = response.data || response;
			setCustomerHistory(data || []);
		} catch (err) {
			console.error('작업 이력 조회 실패:', err);
			setCustomerHistory([]);
		} finally {
			setHistoryLoading(false);
		}
	};

	const handleOpenEditModal = () => {
		if (customerDetail) {
			editForm.setFieldsValue({
				customerName: customerDetail.customerName,
				customerEmail: customerDetail.customerEmail,
				customerBirth: customerDetail.customerBirth ? dayjs(customerDetail.customerBirth, 'YYYY-MM-DD') : null,
			});
			setEditModalVisible(true);
		}
	};

	const handleUpdateCustomer = async () => {
		try {
			await editForm.validateFields();
			// 비활성화된 계정에 대한 경고 강화
			if (customerDetail.accountStatus === 'INACTIVE') {
				Modal.confirm({
					title: '⚠️ 비활성화된 계정 수정',
					content: (
						<div>
							<p><strong>이 계정은 현재 비활성화 상태입니다.</strong></p>
							<p>비활성화된 계정의 정보를 수정하는 것은 고객의 동의 없이 개인정보를 변경하는 행위입니다.</p>
							<p>정말로 수정하시겠습니까? (모든 작업은 이력에 기록됩니다)</p>
						</div>
					),
					okText: '수정하기',
					cancelText: '취소',
					okButtonProps: { danger: true },
					onOk: async () => {
						await performUpdate();
					}
				});
			} else {
				await performUpdate();
			}
		} catch (err) {
			console.error('고객 정보 수정 실패:', err);
			message.error(err.response?.data?.message || '고객 정보 수정에 실패했습니다.');
		}
	};

	const performUpdate = async () => {
		try {
			setUpdating(true);
			const values = editForm.getFieldsValue();
			// 생년월일을 dayjs 객체에서 문자열로 변환 (타임존 문제 방지)
			if (values.customerBirth) {
				values.customerBirth = values.customerBirth.format('YYYY-MM-DD');
			}
			const response = await AdminService.updateCustomer(customerDetail.customerId, values);
			const data = response.data || response;
			setCustomerDetail(data);
			setEditModalVisible(false);
			message.success('고객 정보가 수정되었습니다.');
			// 목록도 새로고침
			fetchCustomerList();
		} catch (err) {
			console.error('고객 정보 수정 실패:', err);
			message.error(err.response?.data?.message || '고객 정보 수정에 실패했습니다.');
		} finally {
			setUpdating(false);
		}
	};

	const handleToggleAccountStatus = async (active) => {
		try {
			await AdminService.updateCustomerAccountStatus(customerDetail.customerId, active);
			message.success(active ? '계정이 활성화되었습니다.' : '계정이 비활성화되었습니다.');
			// 상세 정보 새로고침
			await handleViewDetail(customerDetail.customerId);
		} catch (err) {
			console.error('계정 상태 변경 실패:', err);
			message.error(err.response?.data?.message || '계정 상태 변경에 실패했습니다.');
		}
	};

	const handleResetPassword = async () => {
		// 비활성화된 계정에 대한 경고 강화
		if (customerDetail.accountStatus === 'INACTIVE') {
			Modal.confirm({
				title: '⚠️ 비활성화된 계정 비밀번호 초기화',
				content: (
					<div>
						<p><strong>이 계정은 현재 비활성화 상태입니다.</strong></p>
						<p>비활성화된 계정의 비밀번호를 초기화하는 것은 고객의 동의 없이 계정 정보를 변경하는 행위입니다.</p>
						<p>정말로 초기화하시겠습니까? (비활성화 상태는 유지되며, 모든 작업은 이력에 기록됩니다)</p>
					</div>
				),
				okText: '초기화하기',
				cancelText: '취소',
				okButtonProps: { danger: true },
				onOk: async () => {
					await performResetPassword();
				}
			});
		} else {
			await performResetPassword();
		}
	};

	const performResetPassword = async () => {
		try {
			const response = await AdminService.resetCustomerPassword(customerDetail.customerId);
			const data = response.data || response;
			Modal.success({
				title: '비밀번호 초기화 완료',
				content: `임시 비밀번호: ${data}\n\n이 비밀번호가 고객 이메일로 발송되었습니다.`,
				width: 500,
			});
		} catch (err) {
			console.error('비밀번호 초기화 실패:', err);
			message.error(err.response?.data?.message || '비밀번호 초기화에 실패했습니다.');
		}
	};

	const handleResendEmailVerification = async () => {
		try {
			await AdminService.resendEmailVerification(customerDetail.customerId);
			message.success('이메일 인증 링크가 재발송되었습니다.');
		} catch (err) {
			console.error('이메일 인증 재발송 실패:', err);
			message.error(err.response?.data?.message || '이메일 인증 재발송에 실패했습니다.');
		}
	};

	const formatDate = (dateString) => {
		if (!dateString) return '-';
		return dayjs(dateString).format('YYYY-MM-DD HH:mm:ss');
	};

	const formatCurrency = (amount) => {
		if (!amount) return '₩0';
		return `₩${amount.toLocaleString()}`;
	};

	const handleSearch = () => {
		setPage(0);
		fetchCustomerList();
	};

	const handleReset = () => {
		setSearchKeyword('');
		setEmailVerifiedFilter(null);
		setPage(0);
	};

	const gradeLabelMap = {
		BEGINNER: '초보자',
		SWIMMER: '수영인',
		PRO: '프로',
		MASTER: '마스터',
		LEGEND: '레전드',
	};

	const tableColumns = [
		{
			title: '고객 ID',
			dataIndex: 'customerId',
			key: 'customerId',
			width: 100,
		},
		{
			title: '등급',
			dataIndex: 'customerGradeCode',
			key: 'customerGradeCode',
			width: 120,
			render: (gradeCode) => {
				if (!gradeCode) {
					return <Tag color="default">등급 없음</Tag>;
				}
				const label = gradeLabelMap[gradeCode] || gradeCode;
				return (
					<Tag color="blue">
						{label}
					</Tag>
				);
			},
		},
		{
			title: '이름',
			dataIndex: 'customerName',
			key: 'customerName',
			width: 120,
		},
		{
			title: '이메일',
			dataIndex: 'customerEmail',
			key: 'customerEmail',
			width: 200,
		},
		{
			title: '이메일 인증',
			dataIndex: 'emailChecked',
			key: 'emailChecked',
			width: 120,
			render: (checked) => (
				<Tag color={checked ? 'green' : 'orange'}>
					{checked ? '인증 완료' : '미인증'}
				</Tag>
			),
		},
		{
			title: '계정 상태',
			dataIndex: 'accountStatus',
			key: 'accountStatus',
			width: 120,
			render: (status) => (
				<Tag color={status === 'ACTIVE' ? 'green' : status === 'INACTIVE' ? 'red' : 'default'}>
					{status === 'ACTIVE' ? '활성' : status === 'INACTIVE' ? '비활성' : '-'}
				</Tag>
			),
		},
		{
			title: '주문 건수',
			dataIndex: 'orderCount',
			key: 'orderCount',
			width: 100,
			render: (count) => `${count || 0}건`,
		},
		{
			title: '태그',
			dataIndex: 'tags',
			key: 'tags',
			width: 200,
			render: (tags) => (
				<Space wrap size={[4, 4]}>
					{(tags || []).map((tag) => (
						<Tag key={tag.tagId} color={tag.tagColor}>
							{tag.tagName}
						</Tag>
					))}
					{(!tags || tags.length === 0) && <span style={{ color: '#999' }}>-</span>}
				</Space>
			),
		},
		{
			title: '작업',
			key: 'action',
			width: 100,
			render: (_, record) => (
				<Button
					type="link"
					size="small"
					icon={<EyeOutlined />}
					onClick={() => handleViewDetail(record.customerId)}
				>
					상세보기
				</Button>
			),
		},
	];

	return (
		<div>
			{/* 통계 카드 */}
			<Row gutter={16} style={{ marginBottom: 16 }}>
				<Col xs={24} sm={12} md={6}>
					<Card>
						<Spin spinning={statisticsLoading}>
							<Statistic
								title="전체 고객 수"
								value={statistics?.totalCustomerCount || 0}
								prefix={<UserOutlined />}
								valueStyle={{ color: '#1890ff' }}
								formatter={(value) => value.toLocaleString()}
							/>
						</Spin>
					</Card>
				</Col>
				<Col xs={24} sm={12} md={6}>
					<Card>
						<Spin spinning={statisticsLoading}>
							<Statistic
								title="이메일 인증 완료"
								value={statistics?.emailVerifiedCount || 0}
								prefix={<MailOutlined />}
								valueStyle={{ color: '#52c41a' }}
								formatter={(value) => value.toLocaleString()}
							/>
						</Spin>
					</Card>
				</Col>
				<Col xs={24} sm={12} md={6}>
					<Card>
						<Spin spinning={statisticsLoading}>
							<Statistic
								title="활성 고객"
								value={statistics?.activeCustomerCount || 0}
								prefix={<ShoppingOutlined />}
								valueStyle={{ color: '#722ed1' }}
								formatter={(value) => value.toLocaleString()}
							/>
						</Spin>
					</Card>
				</Col>
				<Col xs={24} sm={12} md={6}>
					<Card>
						<Spin spinning={statisticsLoading}>
							<Statistic
								title="오늘 가입"
								value={statistics?.newCustomerCountToday || 0}
								valueStyle={{ color: '#fa8c16' }}
								formatter={(value) => value.toLocaleString()}
							/>
						</Spin>
					</Card>
				</Col>
			</Row>

			<Card>
				<div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
					<h4 style={{ margin: 0 }}>고객 목록</h4>
					<Space>
						<Input
							placeholder="이름 또는 이메일 검색"
							value={searchKeyword}
							onChange={(e) => setSearchKeyword(e.target.value)}
							onPressEnter={handleSearch}
							style={{ width: 200 }}
							prefix={<SearchOutlined />}
						/>
						<Select
							placeholder="이메일 인증 필터"
							value={emailVerifiedFilter}
							onChange={setEmailVerifiedFilter}
							style={{ width: 150 }}
							allowClear
						>
							<Option value={true}>인증 완료</Option>
							<Option value={false}>미인증</Option>
						</Select>
						<Select
							placeholder="등급 필터"
							value={gradeFilter}
							onChange={setGradeFilter}
							style={{ width: 150 }}
							allowClear
						>
							<Option value="BEGINNER">BEGINNER</Option>
							<Option value="SWIMMER">SWIMMER</Option>
							<Option value="PRO">PRO</Option>
							<Option value="MASTER">MASTER</Option>
							<Option value="LEGEND">LEGEND</Option>
						</Select>
						<Button onClick={handleSearch} icon={<SearchOutlined />}>
							검색
						</Button>
						<Button onClick={handleReset}>
							초기화
						</Button>
						<Button icon={<ReloadOutlined />} onClick={fetchCustomerList}>
							새로고침
						</Button>
					</Space>
				</div>
				<Table
					columns={tableColumns}
					dataSource={customers}
					rowKey="customerId"
					loading={loading}
					pagination={{
						current: page + 1,
						pageSize: pageSize,
						total: totalCount,
						showSizeChanger: true,
						showTotal: (total) => `총 ${total}건`,
						pageSizeOptions: ['10', '20', '50', '100'],
						onChange: (newPage, newPageSize) => {
							setPage(newPage - 1);
							setPageSize(newPageSize);
						},
					}}
				/>
			</Card>

			{/* 고객 상세 모달 */}
			<Modal
				title="고객 상세 정보"
				open={detailModalVisible}
				onCancel={() => {
					setDetailModalVisible(false);
					setSelectedCustomer(null);
					setCustomerDetail(null);
				}}
				footer={[
					<Button key="close" onClick={() => {
						setDetailModalVisible(false);
						setSelectedCustomer(null);
						setCustomerDetail(null);
					}}>
						닫기
					</Button>,
				]}
				width={1000}
			>
				<Spin spinning={customerDetailLoading}>
					{customerDetail && (
						<>
							<div style={{ marginBottom: 16, display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
								<Button icon={<EditOutlined />} onClick={handleOpenEditModal}>
									정보 수정
								</Button>
								{customerDetail.accountStatus === 'ACTIVE' ? (
									<Popconfirm
										title="계정 비활성화"
										description="이 고객의 계정을 비활성화하시겠습니까? 비활성화된 계정은 로그인할 수 없습니다."
										onConfirm={() => handleToggleAccountStatus(false)}
										okText="확인"
										cancelText="취소"
									>
										<Button icon={<LockOutlined />} danger>
											계정 비활성화
										</Button>
									</Popconfirm>
								) : (
									<Popconfirm
										title="계정 활성화"
										description="이 고객의 계정을 활성화하시겠습니까?"
										onConfirm={() => handleToggleAccountStatus(true)}
										okText="확인"
										cancelText="취소"
									>
										<Button icon={<UnlockOutlined />} type="primary">
											계정 활성화
										</Button>
									</Popconfirm>
								)}
								{customerDetail.emailChecked ? null : (
									<Button 
										icon={<SendOutlined />} 
										onClick={handleResendEmailVerification}
									>
										이메일 인증 재발송
									</Button>
								)}
								<Popconfirm
									title="비밀번호 초기화"
									description="고객의 비밀번호를 초기화하시겠습니까? 임시 비밀번호가 이메일로 발송됩니다."
									onConfirm={handleResetPassword}
									okText="확인"
									cancelText="취소"
								>
									<Button icon={<KeyOutlined />} danger>
										비밀번호 초기화
									</Button>
								</Popconfirm>
							</div>
							<Tabs activeKey={detailTab} onChange={setDetailTab}>
								<TabPane tab="기본 정보" key="info">
									<Descriptions bordered column={2} style={{ marginBottom: 24 }}>
										<Descriptions.Item label="고객 ID">{customerDetail.customerId}</Descriptions.Item>
										<Descriptions.Item label="이름">{customerDetail.customerName}</Descriptions.Item>
										<Descriptions.Item label="이메일">{customerDetail.customerEmail}</Descriptions.Item>
										<Descriptions.Item label="생년월일">{customerDetail.customerBirth || '-'}</Descriptions.Item>
										<Descriptions.Item label="가입일">{formatDate(customerDetail.customerCreateAt)}</Descriptions.Item>
										<Descriptions.Item label="이메일 인증">
											<Tag color={customerDetail.emailChecked ? 'green' : 'orange'}>
												{customerDetail.emailChecked ? '인증 완료' : '미인증'}
											</Tag>
										</Descriptions.Item>
										<Descriptions.Item label="계정 상태">
											<Tag color={customerDetail.accountStatus === 'ACTIVE' ? 'green' : customerDetail.accountStatus === 'INACTIVE' ? 'red' : 'default'}>
												{customerDetail.accountStatus === 'ACTIVE' ? '활성' : customerDetail.accountStatus === 'INACTIVE' ? '비활성' : '알 수 없음'}
											</Tag>
										</Descriptions.Item>
										<Descriptions.Item label="고객 등급">
											{customerDetail.customerGrade ? (
												<Tag color="blue" style={{ fontSize: '14px', padding: '4px 12px' }}>
													{customerDetail.customerGrade.gradeName || '-'}
												</Tag>
											) : (
												<Tag color="default">등급 없음</Tag>
											)}
										</Descriptions.Item>
										<Descriptions.Item label="누적 구매액">
											{formatCurrency(customerDetail.totalPurchaseAmount || 0)}
										</Descriptions.Item>
										<Descriptions.Item label="총 주문 건수">{customerDetail.totalOrderCount || 0}건</Descriptions.Item>
										<Descriptions.Item label="총 주문 금액">{formatCurrency(customerDetail.totalOrderAmount)}</Descriptions.Item>
										<Descriptions.Item label="총 리뷰 건수">{customerDetail.totalReviewCount || 0}건</Descriptions.Item>
										<Descriptions.Item label="총 반품 건수">{customerDetail.totalReturnCount || 0}건</Descriptions.Item>
										<Descriptions.Item label="태그" span={2}>
											<Space wrap>
												{(customerDetail.tags || []).map((tag) => (
													<Tag 
														key={tag.tagId} 
														color={tag.tagColor}
														closable
														onClose={() => handleRemoveTagFromCustomer(tag.tagId)}
													>
														{tag.tagName}
													</Tag>
												))}
												<Button 
													type="dashed" 
													size="small" 
													icon={<PlusOutlined />}
													onClick={() => setAddTagModalVisible(true)}
												>
													태그 추가
												</Button>
											</Space>
										</Descriptions.Item>
									</Descriptions>
								</TabPane>
							<TabPane tab={<span><ShoppingOutlined /> 최근 주문</span>} key="orders">
								<Table
									columns={[
										{ title: '주문 번호', dataIndex: 'orderNo', key: 'orderNo' },
										{ title: '주문일', dataIndex: 'orderCreatedAt', key: 'orderCreatedAt', render: (date) => formatDate(date) },
										{ title: '상태', dataIndex: 'orderStatus', key: 'orderStatus' },
										{ title: '상품 수', dataIndex: 'itemCount', key: 'itemCount', render: (count) => `${count}개` },
										{ title: '주문 금액', dataIndex: 'orderTotalPrice', key: 'orderTotalPrice', render: (amount) => formatCurrency(amount) },
									]}
									dataSource={customerDetail.recentOrders || []}
									rowKey="orderNo"
									pagination={false}
									locale={{ emptyText: '주문 내역이 없습니다.' }}
								/>
							</TabPane>
							<TabPane tab={<span><StarOutlined /> 최근 리뷰</span>} key="reviews">
								<Table
									columns={[
										{ title: '리뷰 번호', dataIndex: 'reviewNo', key: 'reviewNo' },
										{ title: '상품명', dataIndex: 'productName', key: 'productName' },
										{ title: '평점', dataIndex: 'rating', key: 'rating', render: (rating) => `${rating}점` },
										{ title: '리뷰 내용', dataIndex: 'reviewContent', key: 'reviewContent', ellipsis: true },
										{ title: '작성일', dataIndex: 'reviewCreatedAt', key: 'reviewCreatedAt', render: (date) => formatDate(date) },
									]}
									dataSource={customerDetail.recentReviews || []}
									rowKey="reviewNo"
									pagination={false}
									locale={{ emptyText: '리뷰 내역이 없습니다.' }}
								/>
							</TabPane>
							<TabPane tab={<span><UndoOutlined /> 최근 반품</span>} key="returns">
								<Table
									columns={[
										{ title: '반품 번호', dataIndex: 'returnNo', key: 'returnNo' },
										{ title: '주문 번호', dataIndex: 'orderNo', key: 'orderNo' },
										{ title: '상품명', dataIndex: 'productName', key: 'productName' },
										{ title: '상태', dataIndex: 'returnStatus', key: 'returnStatus' },
										{ title: '반품 금액', dataIndex: 'returnAmount', key: 'returnAmount', render: (amount) => formatCurrency(amount) },
										{ title: '신청일', dataIndex: 'returnRequestedAt', key: 'returnRequestedAt', render: (date) => formatDate(date) },
									]}
									dataSource={customerDetail.recentReturns || []}
									rowKey="returnNo"
									pagination={false}
									locale={{ emptyText: '반품 내역이 없습니다.' }}
								/>
							</TabPane>
							<TabPane tab={<span><HistoryOutlined /> 작업 이력</span>} key="history">
								<Spin spinning={historyLoading}>
									{customerHistory.length === 0 ? (
										<div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
											작업 이력이 없습니다.
										</div>
									) : (
										<Timeline>
											{customerHistory.map((history) => (
												<Timeline.Item
													key={history.historyId}
													color={
														history.actionType === 'STATUS_DEACTIVATE' ? 'red' :
														history.actionType === 'STATUS_ACTIVATE' ? 'green' :
														history.actionType === 'PASSWORD_RESET' ? 'orange' :
														'blue'
													}
												>
													<div style={{ marginBottom: 8 }}>
														<Space>
															<Tag color={
																history.actionType === 'STATUS_DEACTIVATE' ? 'red' :
																history.actionType === 'STATUS_ACTIVATE' ? 'green' :
																history.actionType === 'PASSWORD_RESET' ? 'orange' :
																'blue'
															}>
																{history.actionTypeLabel || history.actionType}
															</Tag>
															<span style={{ color: '#666' }}>
																{history.adminName} 관리자
															</span>
														</Space>
													</div>
													{history.reason && (
														<div style={{ marginBottom: 4, color: '#666' }}>
															{history.reason}
														</div>
													)}
													<div style={{ fontSize: '12px', color: '#999' }}>
														{formatDate(history.createdAt)}
													</div>
												</Timeline.Item>
											))}
										</Timeline>
									)}
								</Spin>
							</TabPane>
							<TabPane tab={<span><FileTextOutlined /> 메모</span>} key="notes">
								<div style={{ marginBottom: 16, display: 'flex', justifyContent: 'flex-end' }}>
									<Button 
										type="primary" 
										icon={<PlusOutlined />} 
										onClick={() => handleOpenNoteModal(null)}
									>
										메모 작성
									</Button>
								</div>
								<Spin spinning={notesLoading}>
									{customerNotes.length === 0 ? (
										<Empty description="작성된 메모가 없습니다." />
									) : (
										<div>
											{customerNotes.map((note) => (
												<Card
													key={note.noteId}
													size="small"
													style={{ marginBottom: 12 }}
													extra={
														<Space>
															{note.isImportant && (
																<Tag color="red" icon={<ExclamationCircleOutlined />}>
																	중요
																</Tag>
															)}
															<Button 
																type="link" 
																size="small" 
																icon={<EditOutlined />}
																onClick={() => handleOpenNoteModal(note)}
															>
																수정
															</Button>
															<Popconfirm
																title="메모 삭제"
																description="이 메모를 삭제하시겠습니까?"
																onConfirm={() => handleDeleteNote(note.noteId)}
																okText="삭제"
																cancelText="취소"
															>
																<Button 
																	type="link" 
																	size="small" 
																	danger
																	icon={<DeleteOutlined />}
																>
																	삭제
																</Button>
															</Popconfirm>
														</Space>
													}
												>
													<div style={{ whiteSpace: 'pre-wrap', marginBottom: 8 }}>
														{note.noteContent}
													</div>
													<div style={{ fontSize: '12px', color: '#999' }}>
														{note.adminName} 관리자 · {formatDate(note.createdAt)}
														{note.updatedAt && note.updatedAt !== note.createdAt && (
															<span> · 수정됨: {formatDate(note.updatedAt)}</span>
														)}
													</div>
												</Card>
											))}
										</div>
									)}
								</Spin>
							</TabPane>
							<TabPane tab="포인트" key="points">
								<Spin spinning={pointLoading}>
									<div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
										<div>
											<strong>현재 포인트 잔액: </strong>
											{formatCurrency(customerDetail.pointBalance || 0)}
										</div>
										<Space>
											<Button type="primary" onClick={() => handleOpenPointModal('add')}>
												포인트 지급
											</Button>
											<Button danger onClick={() => handleOpenPointModal('deduct')}>
												포인트 차감
											</Button>
										</Space>
									</div>
							<Table
										rowKey="historyId"
										dataSource={pointHistory}
										size="small"
								pagination={{
									current: pointPage,
									pageSize: pointPageSize,
									total: pointTotal,
									showSizeChanger: true,
									showTotal: (t) => `총 ${t}건`,
									onChange: (p, s) => {
										setPointPage(p);
										setPointPageSize(s);
										// 페이지 변경 시 재조회
										if (customerDetail?.customerId) {
											// 비동기 의존 방지: setState 이후 fetch를 호출
											setTimeout(() => fetchCustomerPointHistory(customerDetail.customerId), 0);
										}
									}
								}}
										locale={{ emptyText: '포인트 내역이 없습니다.' }}
										columns={[
											{
												title: '일시',
												dataIndex: 'createdAt',
												key: 'createdAt',
												render: (value) => (value ? formatDate(value) : '-'),
											},
											{
												title: '유형',
												dataIndex: 'pointType',
												key: 'pointType',
											},
											{
												title: '변동 포인트',
												dataIndex: 'pointAmount',
												key: 'pointAmount',
												align: 'right',
												render: (amount) => (amount > 0 ? `+${amount}` : amount),
											},
											{
												title: '잔액',
												dataIndex: 'pointBalanceAfter',
												key: 'pointBalanceAfter',
												align: 'right',
											},
											{
												title: '설명',
												dataIndex: 'description',
												key: 'description',
												ellipsis: true,
											},
											{
												title: '관련 주문',
												key: 'orderRef',
												render: (_, record) => {
													if (record.orderNo) {
														return `주문번호 ${record.orderNo}`;
													}
													if (record.orderItemNo) {
														return `주문상품 ${record.orderItemNo}`;
													}
													return '-';
												},
											},
											{
												title: '관리자',
												dataIndex: 'adminName',
												key: 'adminName',
											},
										]}
									/>
								</Spin>
							</TabPane>
							<TabPane tab={<span><HistoryOutlined /> 활동 로그</span>} key="activity">
								<Spin spinning={activityLogsLoading}>
									{activityLogs.length === 0 ? (
										<Empty description="활동 로그가 없습니다." />
									) : (
										<Timeline>
											{activityLogs.map((log) => (
												<Timeline.Item
													key={log.logId}
													color={
														log.activityType === 'LOGIN' ? 'green' :
														log.activityType === 'ORDER_CREATED' ? 'blue' :
														log.activityType === 'REVIEW_CREATED' ? 'orange' :
														log.activityType === 'RETURN_REQUESTED' ? 'red' :
														'gray'
													}
												>
													<div style={{ marginBottom: 8 }}>
														<Space>
															<Tag color={
																log.activityType === 'LOGIN' ? 'green' :
																log.activityType === 'ORDER_CREATED' ? 'blue' :
																log.activityType === 'REVIEW_CREATED' ? 'orange' :
																log.activityType === 'RETURN_REQUESTED' ? 'red' :
																'default'
															}>
																{log.activityTypeLabel || log.activityType}
															</Tag>
															<span style={{ color: '#666' }}>
																{formatDate(log.activityAt)}
															</span>
														</Space>
													</div>
													{log.activityDetails && (
														<div style={{ marginBottom: 4, color: '#666', fontSize: '12px' }}>
															{log.activityDetails}
														</div>
													)}
													{log.ipAddress && (
														<div style={{ fontSize: '12px', color: '#999' }}>
															IP: {log.ipAddress}
														</div>
													)}
												</Timeline.Item>
											))}
										</Timeline>
									)}
								</Spin>
							</TabPane>
						</Tabs>
						</>
					)}
				</Spin>
			</Modal>

			{/* 포인트 지급/차감 모달 */}
			<Modal
				title={pointModalMode === 'add' ? '포인트 지급' : '포인트 차감'}
				open={pointModalVisible}
				onOk={handleSubmitPoint}
				onCancel={() => {
					setPointModalVisible(false);
					pointForm.resetFields();
				}}
				okText="저장"
				cancelText="취소"
			>
				<Form form={pointForm} layout="vertical">
					<Form.Item
						name="pointAmount"
						label="포인트 금액"
						rules={[
							{ required: true, message: '포인트 금액을 입력해주세요.' },
							{ type: 'number', min: 1, message: '1 이상이어야 합니다.' },
						]}
					>
						<InputNumber min={1} style={{ width: '100%' }} />
					</Form.Item>
					<Form.Item
						name="description"
						label="설명"
						rules={[{ required: true, message: '설명을 입력해주세요.' }]}
					>
						<Input.TextArea rows={3} />
					</Form.Item>
				</Form>
			</Modal>

			{/* 메모 작성/수정 모달 */}
			<Modal
				title={editingNote ? '메모 수정' : '메모 작성'}
				open={noteModalVisible}
				onOk={handleSaveNote}
				onCancel={() => {
					setNoteModalVisible(false);
					setEditingNote(null);
					noteForm.resetFields();
				}}
				okText={editingNote ? '수정' : '작성'}
				cancelText="취소"
				width={600}
			>
				<Form
					form={noteForm}
					layout="vertical"
				>
					<Form.Item
						label="메모 내용"
						name="noteContent"
						rules={[
							{ required: true, message: '메모 내용을 입력해주세요.' },
							{ max: 2000, message: '메모는 2000자 이하여야 합니다.' }
						]}
					>
						<Input.TextArea 
							rows={6} 
							placeholder="고객에 대한 메모를 입력하세요..."
							showCount
							maxLength={2000}
						/>
					</Form.Item>
					<Form.Item
						name="isImportant"
						valuePropName="checked"
					>
						<Checkbox>중요 메모로 표시</Checkbox>
					</Form.Item>
				</Form>
			</Modal>

			{/* 고객 정보 수정 모달 */}
			<Modal
				title="고객 정보 수정"
				open={editModalVisible}
				onOk={handleUpdateCustomer}
				onCancel={() => {
					setEditModalVisible(false);
					editForm.resetFields();
				}}
				confirmLoading={updating}
				okText="수정"
				cancelText="취소"
			>
				<Form
					form={editForm}
					layout="vertical"
				>
					<Form.Item
						label="고객 이름"
						name="customerName"
						rules={[
							{ required: true, message: '고객 이름을 입력해주세요.' },
							{ min: 2, message: '이름은 2자 이상이어야 합니다.' },
						]}
					>
						<Input placeholder="고객 이름을 입력하세요" />
					</Form.Item>
					<Form.Item
						label="이메일"
						name="customerEmail"
						rules={[
							{ required: true, message: '이메일을 입력해주세요.' },
							{ type: 'email', message: '올바른 이메일 형식이 아닙니다.' },
						]}
					>
						<Input placeholder="이메일을 입력하세요" />
					</Form.Item>
					<Form.Item
						label="생년월일"
						name="customerBirth"
					>
						<DatePicker 
							style={{ width: '100%' }}
							format="YYYY-MM-DD"
							placeholder="생년월일을 선택하세요"
						/>
					</Form.Item>
				</Form>
			</Modal>

			{/* 태그 추가 모달 */}
			<Modal
				title="태그 추가"
				open={addTagModalVisible}
				onCancel={() => setAddTagModalVisible(false)}
				footer={null}
				width={600}
			>
				<Spin spinning={tagsLoading}>
					<div style={{ marginBottom: 16 }}>
						<Button 
							type="primary" 
							icon={<PlusOutlined />}
							onClick={() => handleOpenTagModal(null)}
						>
							새 태그 생성
						</Button>
					</div>
					{allTags.length === 0 ? (
						<Empty description="생성된 태그가 없습니다. 먼저 태그를 생성해주세요." />
					) : (
						<div>
							{allTags.map((tag) => {
								const hasTag = (customerDetail?.tags || []).some(t => t.tagId === tag.tagId);
								return (
									<Tag
										key={tag.tagId}
										color={tag.tagColor}
										style={{ 
											marginBottom: 8, 
											cursor: hasTag ? 'not-allowed' : 'pointer',
											opacity: hasTag ? 0.5 : 1
										}}
										onClick={() => !hasTag && handleAddTagToCustomer(tag.tagId)}
									>
										{tag.tagName}
										{hasTag && ' (추가됨)'}
									</Tag>
								);
							})}
						</div>
					)}
				</Spin>
			</Modal>

			{/* 태그 생성/수정 모달 */}
			<Modal
				title={editingTag ? '태그 수정' : '태그 생성'}
				open={tagModalVisible}
				onOk={handleSaveTag}
				onCancel={() => {
					setTagModalVisible(false);
					setEditingTag(null);
					tagForm.resetFields();
				}}
				okText={editingTag ? '수정' : '생성'}
				cancelText="취소"
				width={600}
			>
				<Form
					form={tagForm}
					layout="vertical"
				>
					<Form.Item
						label="태그 이름"
						name="tagName"
						rules={[
							{ required: true, message: '태그 이름을 입력해주세요.' },
							{ max: 50, message: '태그 이름은 50자 이하여야 합니다.' }
						]}
					>
						<Input placeholder="태그 이름을 입력하세요" />
					</Form.Item>
					<Form.Item
						label="태그 색상"
						name="tagColor"
						rules={[
							{ required: true, message: '태그 색상을 선택해주세요.' },
							{ pattern: /^#[0-9A-Fa-f]{6}$/, message: '올바른 색상 코드 형식이 아닙니다. (예: #ff4d4f)' }
						]}
					>
						<Space>
							<Input 
								type="color" 
								style={{ width: 60, height: 32 }}
								onChange={(e) => tagForm.setFieldsValue({ tagColor: e.target.value })}
							/>
							<Input 
								placeholder="#1890ff"
								style={{ width: 120 }}
								onChange={(e) => {
									const value = e.target.value;
									if (value.startsWith('#')) {
										tagForm.setFieldsValue({ tagColor: value });
									}
								}}
							/>
						</Space>
					</Form.Item>
					<Form.Item
						label="태그 설명"
						name="description"
						rules={[
							{ max: 200, message: '태그 설명은 200자 이하여야 합니다.' }
						]}
					>
						<Input.TextArea 
							rows={3} 
							placeholder="태그 설명을 입력하세요 (선택사항)"
							maxLength={200}
							showCount
						/>
					</Form.Item>
				</Form>
			</Modal>
		</div>
	);
};

export default CustomerManagement;
