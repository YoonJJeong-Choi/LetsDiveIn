import React, { useState, useEffect } from 'react';
import { Card, Form, Input, Button, Alert, Tabs, Timeline, Tag, Space, Select, message, Spin, Table, Modal, Upload, Avatar, Row, Col } from 'antd';
import { ReloadOutlined, HistoryOutlined } from '@ant-design/icons';
import PartnerService from 'services/PartnerService';
import FileService from 'services/FileService';
import { useSelector } from 'react-redux';
import { useLocation } from 'react-router-dom';
import { ROW_GUTTER } from 'constants/ThemeConstant';

const { TextArea } = Input;
const { Option } = Select;
const { TabPane } = Tabs;

const PartnerSettings = () => {
	const { user } = useSelector(state => state.auth);
	const [loading, setLoading] = useState(false);
	const [initialLoading, setInitialLoading] = useState(true);
	const [partnerInfo, setPartnerInfo] = useState(null);
	const [history, setHistory] = useState([]);
	const [profile, setProfile] = useState(null);
	const [changeRequests, setChangeRequests] = useState([]);
	const location = useLocation();
	const [activeTab, setActiveTab] = useState('profile');
	const [historyActionTypeFilter, setHistoryActionTypeFilter] = useState(null);
	const [profileLoading, setProfileLoading] = useState(false);
	const [changeRequestLoading, setChangeRequestLoading] = useState(false);
	const [deactivationForm] = Form.useForm();
	const [reactivationForm] = Form.useForm();
	const [profileForm] = Form.useForm();
	const [changeRequestForm] = Form.useForm();

	// URL 쿼리로 탭 지정 지원 (?tab=profile|profile-change-request|request|history)
	useEffect(() => {
		const params = new URLSearchParams(location.search);
		const tab = params.get('tab');
		if (tab) setActiveTab(tab);
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, []);

	// 파트너 정보 및 이력 로드
	useEffect(() => {
		const fetchData = async () => {
			try {
				setInitialLoading(true);
				// 파트너 상세 정보 조회
				try {
					const partnerData = await PartnerService.getMyPartnerInfo();
					const partner = partnerData?.data || partnerData;
					setPartnerInfo(partner);
					// 고위험 신청 폼 초기값 설정
					changeRequestForm.setFieldsValue({
						requestedPartnerName: partner?.partnerName || '',
						requestedPartnerBankAccount: partner?.partnerBankAccount || '',
						requestedBusinessRegistrationNumber: partner?.businessRegistrationNumber || '',
						requestReason: ''
					});
				} catch (err) {
					console.error('파트너 상세 정보 조회 실패:', err);
					message.error('파트너 정보를 불러오는데 실패했습니다.');
				}

				// 이력 조회
				try {
					const historyData = await PartnerService.getMyHistory();
					const historyList = historyData?.data || historyData;
					setHistory(Array.isArray(historyList) ? historyList : []);
				} catch (err) {
					console.error('이력 조회 실패:', err);
				}

				try {
					const profileData = await PartnerService.getMyProfile();
					const profileResult = profileData?.data || profileData;
					setProfile(profileResult);
					profileForm.setFieldsValue({
						contactPersonName: profileResult?.contactPersonName || '',
						customerServicePhone: profileResult?.customerServicePhone || '',
						profileImageUrl: profileResult?.profileImageUrl || '',
						introduction: profileResult?.introduction || '',
					});
				} catch (err) {
					console.error('프로필 조회 실패:', err);
				}

				try {
					const requestsData = await PartnerService.getMyProfileChangeRequests();
					const requests = requestsData?.data || requestsData;
					setChangeRequests(Array.isArray(requests) ? requests : []);
				} catch (err) {
					console.error('변경 신청 조회 실패:', err);
				}
			} catch (error) {
				console.error('데이터 로드 실패:', error);
			} finally {
				setInitialLoading(false);
			}
		};
		fetchData();
	}, []);

	// 휴업 신청
	const handleDeactivationRequest = async (values) => {
		if (!values.deactivationReason || values.deactivationReason.trim().length < 10) {
			message.error('휴업 신청 사유는 최소 10자 이상 입력해주세요.');
			return;
		}

		try {
			setLoading(true);
			await PartnerService.requestDeactivation({
				deactivationReason: values.deactivationReason.trim(),
			});
			message.success('휴업 신청이 완료되었습니다. 관리자 승인을 기다려주세요.');
			deactivationForm.resetFields();
			// 파트너 정보 다시 로드
			const partnerData = await PartnerService.getMyPartnerInfo();
			const partner = partnerData?.data || partnerData;
			setPartnerInfo(partner);
			// 이력 다시 로드
			const historyData = await PartnerService.getMyHistory();
			const historyList = historyData?.data || historyData;
			setHistory(Array.isArray(historyList) ? historyList : []);
		} catch (error) {
			message.error(error.response?.data?.message || error.message || '휴업 신청에 실패했습니다.');
		} finally {
			setLoading(false);
		}
	};

	// 재활성화 신청
	const handleReactivationRequest = async (values) => {
		if (!values.reactivationReason || values.reactivationReason.trim().length < 10) {
			message.error('재활성화 신청 사유는 최소 10자 이상 입력해주세요.');
			return;
		}

		try {
			setLoading(true);
			await PartnerService.requestReactivation({
				reactivationReason: values.reactivationReason.trim(),
			});
			message.success('재활성화 신청이 완료되었습니다. 관리자 승인을 기다려주세요.');
			reactivationForm.resetFields();
			// 파트너 정보 다시 로드
			const partnerData = await PartnerService.getMyPartnerInfo();
			const partner = partnerData?.data || partnerData;
			setPartnerInfo(partner);
			// 이력 다시 로드
			const historyData = await PartnerService.getMyHistory();
			const historyList = historyData?.data || historyData;
			setHistory(Array.isArray(historyList) ? historyList : []);
		} catch (error) {
			message.error(error.response?.data?.message || error.message || '재활성화 신청에 실패했습니다.');
		} finally {
			setLoading(false);
		}
	};

	// 이력 필터 변경
	const handleHistoryFilterChange = async (actionType) => {
		setHistoryActionTypeFilter(actionType);
		try {
			const historyData = await PartnerService.getMyHistory({ actionType: actionType || null });
			const historyList = historyData?.data || historyData;
			setHistory(Array.isArray(historyList) ? historyList : []);
		} catch (err) {
			console.error('이력 필터링 실패:', err);
		}
	};

	const loadProfile = async () => {
		const profileData = await PartnerService.getMyProfile();
		const profileResult = profileData?.data || profileData;
		setProfile(profileResult);
		profileForm.setFieldsValue({
			contactPersonName: profileResult?.contactPersonName || '',
			customerServicePhone: profileResult?.customerServicePhone || '',
			profileImageUrl: profileResult?.profileImageUrl || '',
			introduction: profileResult?.introduction || '',
		});
	};

	const loadChangeRequests = async () => {
		const requestsData = await PartnerService.getMyProfileChangeRequests();
		const requests = requestsData?.data || requestsData;
		setChangeRequests(Array.isArray(requests) ? requests : []);
	};

	const handleUpdateProfile = async (values) => {
		Modal.confirm({
			title: '프로필을 수정하시겠습니까?',
			okText: '수정',
			cancelText: '취소',
			onOk: async () => {
				try {
					setProfileLoading(true);
					await PartnerService.updateMyProfile({
						contactPersonName: values.contactPersonName?.trim() || null,
						customerServicePhone: values.customerServicePhone?.trim() || null,
						profileImageUrl: values.profileImageUrl?.trim() || null,
						introduction: values.introduction?.trim() || null,
					});
					await loadProfile();
					message.success('프로필이 수정되었습니다.');
				} catch (error) {
					message.error(error.response?.data?.message || error.message || '프로필 수정에 실패했습니다.');
				} finally {
					setProfileLoading(false);
				}
			},
		});
	};

	const beforeUploadImage = (file) => {
		const isAllowed = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type);
		if (!isAllowed) {
			message.error('이미지 파일만 업로드할 수 있습니다 (jpg, png, gif, webp).');
			return Upload.LIST_IGNORE;
		}
		const isLt5M = file.size / 1024 / 1024 < 5;
		if (!isLt5M) {
			message.error('파일 용량은 5MB 이하만 허용됩니다.');
			return Upload.LIST_IGNORE;
		}
		return true;
	};

	const handleUploadProfileImage = async ({ file }) => {
		try {
			setProfileLoading(true);
			const res = await FileService.uploadFile(file, 'profile');
			const data = res?.data || res;
			if (data?.fileUrl) {
				profileForm.setFieldsValue({ profileImageUrl: data.fileUrl });
				message.success('프로필 이미지가 업로드되었습니다. 저장을 눌러 반영하세요.');
			} else {
				message.error('업로드 응답을 확인할 수 없습니다.');
			}
		} catch (e) {
			message.error(e?.response?.data?.message || e?.message || '프로필 이미지 업로드에 실패했습니다.');
		} finally {
			setProfileLoading(false);
		}
	};

	const handleCreateChangeRequest = async (values) => {
		Modal.confirm({
			title: '정보 변경을 신청하시겠습니까?',
			okText: '신청',
			cancelText: '취소',
			onOk: async () => {
				try {
					setChangeRequestLoading(true);
					const payload = {
						requestedPartnerName: values.requestedPartnerName?.trim() || null,
						requestedPartnerBankAccount: values.requestedPartnerBankAccount?.trim() || null,
						requestedBusinessRegistrationNumber: values.requestedBusinessRegistrationNumber?.trim() || null,
						requestReason: values.requestReason?.trim() || '',
					};
					await PartnerService.createProfileChangeRequest(payload);
					await loadChangeRequests();
					message.success('정보 변경 신청이 등록되었습니다.');
				} catch (error) {
					message.error(error.response?.data?.message || error.message || '정보 변경 신청에 실패했습니다.');
				} finally {
					setChangeRequestLoading(false);
				}
			}
		});
	};

	// 이력 액션 타입 한글 변환
	const getActionLabel = (actionType) => {
		const labels = {
			APPLICATION: '입점 신청',
			APPROVAL: '입점 승인',
			REJECTION: '입점 거절',
			DEACTIVATION_REQUEST: '휴업 신청',
			DEACTIVATION_APPROVED: '휴업 승인',
			DEACTIVATION_REJECTION: '휴업 신청 거절',
			REACTIVATION_REQUEST: '재활성화 신청',
			REACTIVATION_APPROVED: '재활성화 승인',
			REACTIVATION_REJECTION: '재활성화 신청 거절',
			DEACTIVATED: '비활성화',
			ACTIVATED: '재활성화',
		};
		return labels[actionType] || actionType;
	};

	// 파트너가 아닌 경우 접근 차단
	if (user && user.role !== 'PARTNER') {
		return (
			<Card>
				<Alert
					message="접근 권한 없음"
					description="이 페이지는 파트너만 접근할 수 있습니다."
					type="error"
					showIcon
				/>
			</Card>
		);
	}

	// 초기 로딩 중
	if (initialLoading) {
		return (
			<Card>
				<Spin spinning={true} tip="로딩 중...">
					<div style={{ minHeight: '200px' }} />
				</Spin>
			</Card>
		);
	}

	// 파트너 상태 확인
	const partnerStatus = partnerInfo?.partnerStatus;
	const canRequestDeactivation = partnerStatus === 'APPROVED' && !partnerInfo?.deactivationRequestedAt;
	const canRequestReactivation = partnerStatus === 'INACTIVE' && !partnerInfo?.reactivationRequestedAt;
	const changeRequestColumns = [
		{
			title: '요청일',
			dataIndex: 'createdAt',
			key: 'createdAt',
			render: (value) => value ? new Date(value).toLocaleString('ko-KR') : '-',
		},
		{
			title: '요청 내용',
			key: 'requestedValues',
			render: (_, record) => {
				const parts = [];
				if (record.requestedPartnerName) parts.push(`상호명: ${record.requestedPartnerName}`);
				if (record.requestedPartnerBankAccount) parts.push(`정산계좌: ${record.requestedPartnerBankAccount}`);
				if (record.requestedBusinessRegistrationNumber) parts.push(`사업자등록번호: ${record.requestedBusinessRegistrationNumber}`);
				if (record.requestedRepresentativeBrandCode) parts.push(`대표 브랜드 코드: ${record.requestedRepresentativeBrandCode}`);
				return parts.length > 0 ? parts.join(' / ') : '-';
			},
		},
		{
			title: '상태',
			dataIndex: 'status',
			key: 'status',
			render: (status) => {
				if (status === 'APPROVED') return <Tag color="green">승인</Tag>;
				if (status === 'REJECTED') return <Tag color="red">거절</Tag>;
				return <Tag color="orange">대기</Tag>;
			},
		},
		{
			title: '사유',
			dataIndex: 'requestReason',
			key: 'requestReason',
			render: (value) => value || '-',
		},
		{
			title: '거절 사유',
			dataIndex: 'rejectReason',
			key: 'rejectReason',
			render: (value) => value || '-',
		},
	];

	return (
		<Card title="파트너 설정">
			<Tabs activeKey={activeTab} onChange={setActiveTab}>
				<TabPane tab="프로필 수정" key="profile">
					<Form
						form={profileForm}
						layout="vertical"
						onFinish={handleUpdateProfile}
						initialValues={{
							contactPersonName: profile?.contactPersonName || '',
							customerServicePhone: profile?.customerServicePhone || '',
							profileImageUrl: profile?.profileImageUrl || '',
							introduction: profile?.introduction || '',
						}}
					>
						{/* 헤더 영역: 템플릿 스킨 차용 (아바타 + 액션 버튼) */}
						<Space align="center" style={{ marginBottom: 16 }} wrap>
							<Avatar
								size={90}
								src={profileForm.getFieldValue('profileImageUrl') || profile?.profileImageUrl}
							/>
							<div className="ml-2">
								<Upload
									maxCount={1}
									beforeUpload={beforeUploadImage}
									customRequest={handleUploadProfileImage}
									showUploadList={false}
								>
									<Button type="primary">프로필 사진 변경</Button>
								</Upload>
								<Button
									className="ml-2"
									onClick={() => {
										profileForm.setFieldsValue({ profileImageUrl: '' });
									}}
								>
									제거
								</Button>
							</div>
						</Space>

						{/* 폼 영역: 2열 레이아웃 차용 */}
						<Row gutter={ROW_GUTTER}>
							<Col xs={24} sm={24} md={12}>
								<Form.Item name="contactPersonName" label="담당자명">
									<Input placeholder="예: 김수영" maxLength={100} />
								</Form.Item>
							</Col>
							<Col xs={24} sm={24} md={12}>
								<Form.Item name="customerServicePhone" label="고객센터 연락처">
									<Input placeholder="예: 02-1234-5678" maxLength={50} />
								</Form.Item>
							</Col>
							<Col xs={24} sm={24} md={24}>
								<Form.Item name="introduction" label="소개">
									<TextArea rows={4} maxLength={1000} showCount />
								</Form.Item>
							</Col>
							<Col xs={24}>
								<Form.Item name="profileImageUrl" label="프로필 이미지 URL">
									<Input placeholder="업로드로 설정됩니다" readOnly />
								</Form.Item>
							</Col>
						</Row>

						<Space>
							<Button type="primary" htmlType="submit" loading={profileLoading}>
								프로필 저장
							</Button>
							<Button onClick={loadProfile}>다시 불러오기</Button>
						</Space>
					</Form>
				</TabPane>

				<TabPane tab="정보 변경 신청" key="profile-change-request">
					<Alert
						type="warning"
						showIcon
						message="해당 페이지는 고위험 정보 변경 신청을 접수합니다."
						description="변경 신청은 관리자 검토 후 반영됩니다."
						style={{ marginBottom: 16 }}
					/>
					<Form
						form={changeRequestForm}
						layout="vertical"
						onFinish={handleCreateChangeRequest}
						initialValues={{
							requestedPartnerName: partnerInfo?.partnerName || '',
							requestedPartnerBankAccount: partnerInfo?.partnerBankAccount || '',
							requestedBusinessRegistrationNumber: partnerInfo?.businessRegistrationNumber || '',
							requestReason: ''
						}}
					>
						<Form.Item name="requestedPartnerName" label="변경 상호명">
							<Input maxLength={255} />
						</Form.Item>
						<Form.Item name="requestedPartnerBankAccount" label="변경 정산계좌">
							<Input maxLength={255} />
						</Form.Item>
						<Form.Item name="requestedBusinessRegistrationNumber" label="변경 사업자등록번호">
							<Input maxLength={100} />
						</Form.Item>
						<Form.Item
							name="requestReason"
							label="변경 사유"
							rules={[{ required: true, message: '변경 사유를 입력해주세요.' }]}
						>
							<TextArea rows={4} maxLength={1000} showCount />
						</Form.Item>
						<Form.Item>
							<Button type="primary" htmlType="submit" loading={changeRequestLoading}>
								변경 신청 등록
							</Button>
						</Form.Item>
					</Form>

					<Table
						rowKey="requestId"
						columns={changeRequestColumns}
						dataSource={changeRequests}
						pagination={{ pageSize: 5 }}
						locale={{ emptyText: '변경 신청 내역이 없습니다.' }}
					/>
				</TabPane>

				<TabPane tab="신청" key="request">
					{/* 현재 상태 표시 */}
					<Alert
						message={
							<>
								<strong>현재 상태:</strong>{' '}
								{partnerStatus === 'APPROVED' && '승인 · 운영 중'}
								{partnerStatus === 'INACTIVE' && '승인 · 비활성'}
								{partnerStatus === 'PENDING' && '입점 신청'}
								{partnerStatus === 'REJECTED' && '거절'}
								{partnerInfo?.deactivationRequestedAt && (
									<Tag color="orange" style={{ marginLeft: 8 }}>
										휴업 신청 대기 중
									</Tag>
								)}
								{partnerInfo?.reactivationRequestedAt && (
									<Tag color="blue" style={{ marginLeft: 8 }}>
										재활성화 신청 대기 중
									</Tag>
								)}
							</>
						}
						type="info"
						showIcon
						style={{ marginBottom: 24 }}
					/>

					{/* 휴업 신청 폼 */}
					{canRequestDeactivation && (
						<Card title="휴업 신청" style={{ marginBottom: 24 }}>
							<p style={{ color: '#666', marginBottom: 16 }}>
								운영 중인 파트너만 휴업 신청이 가능합니다. 관리자 승인 후 휴업이 처리됩니다.
							</p>
							<Form
								form={deactivationForm}
								onFinish={handleDeactivationRequest}
								layout="vertical"
							>
								<Form.Item
									name="deactivationReason"
									label="휴업 신청 사유"
									rules={[
										{ required: true, message: '휴업 신청 사유를 입력해주세요.' },
										{ min: 10, message: '휴업 신청 사유는 최소 10자 이상 입력해주세요.' },
										{ max: 500, message: '휴업 신청 사유는 최대 500자까지 입력 가능합니다.' },
									]}
								>
									<TextArea
										rows={5}
										placeholder="휴업 신청 사유를 입력해주세요. (최소 10자 이상)"
										maxLength={500}
										showCount
									/>
								</Form.Item>
								<Form.Item>
									<Button type="primary" htmlType="submit" loading={loading}>
										휴업 신청
									</Button>
								</Form.Item>
							</Form>
						</Card>
					)}

					{/* 재활성화 신청 폼 */}
					{canRequestReactivation && (
						<Card title="재활성화 신청">
							<p style={{ color: '#666', marginBottom: 16 }}>
								비활성 상태인 파트너만 재활성화 신청이 가능합니다. 관리자 승인 후 재활성화가 처리됩니다.
							</p>
							<Form
								form={reactivationForm}
								onFinish={handleReactivationRequest}
								layout="vertical"
							>
								<Form.Item
									name="reactivationReason"
									label="재활성화 신청 사유"
									rules={[
										{ required: true, message: '재활성화 신청 사유를 입력해주세요.' },
										{ min: 10, message: '재활성화 신청 사유는 최소 10자 이상 입력해주세요.' },
										{ max: 500, message: '재활성화 신청 사유는 최대 500자까지 입력 가능합니다.' },
									]}
								>
									<TextArea
										rows={5}
										placeholder="재활성화 신청 사유를 입력해주세요. (최소 10자 이상)"
										maxLength={500}
										showCount
									/>
								</Form.Item>
								<Form.Item>
									<Button type="primary" htmlType="submit" loading={loading}>
										재활성화 신청
									</Button>
								</Form.Item>
							</Form>
						</Card>
					)}

					{/* 신청 불가 안내 */}
					{!canRequestDeactivation && !canRequestReactivation && (
						<Alert
							message="신청 불가"
							description={
								<>
									{partnerStatus === 'PENDING' && '입점 신청이 승인되면 휴업 신청이 가능합니다.'}
									{partnerStatus === 'REJECTED' && '입점이 거절된 상태입니다. 재신청이 필요합니다.'}
									{partnerInfo?.deactivationRequestedAt && '휴업 신청이 이미 접수되었습니다. 관리자 승인을 기다려주세요.'}
									{partnerInfo?.reactivationRequestedAt && '재활성화 신청이 이미 접수되었습니다. 관리자 승인을 기다려주세요.'}
								</>
							}
							type="warning"
							showIcon
						/>
					)}
				</TabPane>

				<TabPane
					tab={
						<span>
							<HistoryOutlined />
							이력 ({history.length})
						</span>
					}
					key="history"
				>
					<Space style={{ marginBottom: 16 }} wrap>
						<Select
							placeholder="이력 유형 선택"
							allowClear
							style={{ width: 200 }}
							onChange={handleHistoryFilterChange}
							value={historyActionTypeFilter}
						>
							<Option value="APPLICATION">입점 신청</Option>
							<Option value="APPROVAL">입점 승인</Option>
							<Option value="REJECTION">입점 거절</Option>
							<Option value="DEACTIVATION_REQUEST">휴업 신청</Option>
							<Option value="DEACTIVATION_APPROVED">휴업 승인</Option>
							<Option value="DEACTIVATION_REJECTION">휴업 신청 거절</Option>
							<Option value="REACTIVATION_REQUEST">재활성화 신청</Option>
							<Option value="REACTIVATION_APPROVED">재활성화 승인</Option>
							<Option value="REACTIVATION_REJECTION">재활성화 신청 거절</Option>
							<Option value="DEACTIVATED">비활성화</Option>
							<Option value="ACTIVATED">재활성화</Option>
						</Select>
						<Button
							icon={<ReloadOutlined />}
							onClick={() => handleHistoryFilterChange(null)}
							size="small"
						>
							전체 보기
						</Button>
					</Space>

					<Spin spinning={loading}>
						{history.length === 0 ? (
							<Alert
								message="이력이 없습니다"
								description={
									historyActionTypeFilter
										? '선택한 이력 유형의 기록이 없습니다.'
										: '파트너의 상태 변경 이력이 없습니다.'
								}
								type="info"
								showIcon
							/>
						) : (
							<Timeline>
								{history.map((item) => {
									const isPartnerAction = [
										'APPLICATION',
										'DEACTIVATION_REQUEST',
										'REACTIVATION_REQUEST',
									].includes(item.actionType);

									const getActionColor = (actionType, isPartner) => {
										if (isPartner) {
											return 'blue';
										}
										switch (actionType) {
											case 'APPROVAL':
											case 'REACTIVATION_APPROVED':
											case 'ACTIVATED':
												return 'green';
											case 'REJECTION':
											case 'DEACTIVATION_REJECTION':
											case 'REACTIVATION_REJECTION':
												return 'red';
											case 'DEACTIVATION_APPROVED':
											case 'DEACTIVATED':
												return 'gray';
											default:
												return 'purple';
										}
									};

									return (
										<Timeline.Item key={item.historyId} color={getActionColor(item.actionType, isPartnerAction)}>
											<div style={{ marginBottom: 8 }}>
												<strong>{getActionLabel(item.actionType)}</strong>
												{isPartnerAction && (
													<Tag color="blue" style={{ marginLeft: 8 }}>
														파트너
													</Tag>
												)}
												{!isPartnerAction && (
													<Tag color="purple" style={{ marginLeft: 8 }}>
														관리자
													</Tag>
												)}
											</div>
											{item.reason && (
												<div style={{ marginBottom: 4, color: '#595959' }}>
													사유: {item.reason}
												</div>
											)}
											{item.adminName && (
												<div style={{ marginBottom: 4, color: '#8c8c8c', fontSize: '12px' }}>
													처리자: {item.adminName}
												</div>
											)}
											<div style={{ color: '#8c8c8c', fontSize: '12px' }}>
												{new Date(item.createdAt).toLocaleString('ko-KR')}
											</div>
										</Timeline.Item>
									);
								})}
							</Timeline>
						)}
					</Spin>
				</TabPane>
			</Tabs>
		</Card>
	);
};

export default PartnerSettings;
