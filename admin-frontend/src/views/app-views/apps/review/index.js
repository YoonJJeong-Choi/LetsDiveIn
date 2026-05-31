import React, { useState, useEffect } from 'react';
import { useSelector } from 'react-redux';
import { Card, Table, Button, Modal, message, Tag, Space, Row, Col, Select, Descriptions, Rate, Image, Input, Statistic, Form, Popconfirm, DatePicker, Spin, Tabs, Segmented, Typography, Alert, Tooltip } from 'antd';
import { CommentOutlined, EyeOutlined, StarOutlined, UserOutlined, ShoppingCartOutlined, DeleteOutlined, EditOutlined, ThunderboltOutlined, CalendarOutlined } from '@ant-design/icons';
import ReviewService from 'services/ReviewService';
import PartnerService from 'services/PartnerService';

const { Option } = Select;
const { RangePicker } = DatePicker;
const { Search } = Input;
const { TextArea } = Input;

const getRatingColor = (rating) => {
	switch (rating) {
		case 5:
			return 'green';
		case 4:
			return 'cyan';
		case 3:
			return 'orange';
		case 2:
		case 1:
			return 'red';
		default:
			return 'default';
	}
};

const ReviewManagement = () => {
	const MIN_ANALYSIS_REVIEWS = 3;
	const user = useSelector((state) => state.auth.user);
	const userRole = user?.role || 'ADMIN'; // 기본값 ADMIN
	
	const [reviews, setReviews] = useState([]);
	const [totalItems, setTotalItems] = useState(0);
	const [currentPage, setCurrentPage] = useState(1);
	const [pageSize, setPageSize] = useState(20);
	const [loading, setLoading] = useState(false);
	const [detailModalVisible, setDetailModalVisible] = useState(false);
	const [replyModalVisible, setReplyModalVisible] = useState(false);
	const [selectedReview, setSelectedReview] = useState(null);
	const [ratingFilter, setRatingFilter] = useState('ALL');
	const [searchText, setSearchText] = useState('');
	const [showUnansweredOnly, setShowUnansweredOnly] = useState(false);
	const [analysisLoading, setAnalysisLoading] = useState(false);
	const [analysisResult, setAnalysisResult] = useState(null);
	const [analysisErrorMessage, setAnalysisErrorMessage] = useState('');
	const [form] = Form.useForm();
	const [products, setProducts] = useState([]);
	const [selectedProductNos, setSelectedProductNos] = useState([]);
	const [selectedOptionNos, setSelectedOptionNos] = useState([]);
	const [dateRange, setDateRange] = useState([]);
	const [analysisDateMode, setAnalysisDateMode] = useState('latest');
	/** 서버 `ai.review-analysis` 설정과 동기화(첫 후보 조회 응답으로 갱신) */
	const [aiSampleMeta, setAiSampleMeta] = useState({ recentCountDefault: 3, minimumRequired: 3, hardCap: 10, requestedLimit: 3 });
	const [reviewTabKey, setReviewTabKey] = useState('list');
	/** whole=모든 옵션 합산, one=옵션 1개만, some=직접 고른 여러 옵션 */
	const [aiOptionScope, setAiOptionScope] = useState('whole');

	const renderOptionLabel = (opt) => {
		if (!opt) return '-';
		// 백엔드에서 한글 optionName이 내려오면 그대로 사용
		if (opt.optionName) return opt.optionName;
		// optionName이 없으면 다양한 필드명을 포함해 color/size 조합으로 표시 (예: 블랙 / M)
		const color = opt.color || opt.colorName || opt.optionColor || opt?.colorEntity?.colorName;
		const size = opt.size || opt.sizeName || opt.optionSize;
		const parts = [color, size].filter(Boolean);
		if (parts.length > 0) return parts.join(' / ');
		if (opt.displayName) return opt.displayName;
		return `옵션 #${opt.optionNo}`;
	};

	useEffect(() => {
		fetchAllReviews(currentPage, pageSize);
	// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [currentPage, pageSize]);

	// 파트너 상품 목록 조회 (단일 product 선택 + option 선택용)
	useEffect(() => {
		const loadProducts = async () => {
			try {
				if (userRole !== 'PARTNER') return;
				const res = await (await import('services/PartnerService')).default.getMyProducts();
				const data = res?.data ?? res ?? [];
				const items = Array.isArray(data) ? data : (data?.items ?? []);
				setProducts(Array.isArray(items) ? items : []);
			} catch (err) {
				// 상품 목록 실패는 치명적이지 않으므로 경고만
				console.warn('파트너 상품 목록 로드 실패:', err?.message || err);
			}
		};
		loadProducts();
	// eslint-disable-next-line react-hooks/exhaustive-deps
	}, []);

	// 필터 변경 시 1페이지로 리셋 후 재조회 (서버 필터 미지원 시 로컬로 제한 적용)
	useEffect(() => {
		setCurrentPage(1);
		// 서버가 필터를 지원하지 않는다면 페이지 첫 화면에서만 로컬 필터가 적용됩니다.
	}, [ratingFilter, searchText, showUnansweredOnly]);

	const fetchAllReviews = async (page = 1, size = 20) => {
		try {
			setLoading(true);
			// 역할에 따라 다른 API 사용
			let response;
			if (userRole === 'PARTNER') {
				console.log('[Partner Reviews] request params =>', { page, size });
				response = await ReviewService.getReviewsByPartner({ page, size });
			} else {
				console.log('[Admin Reviews] request params =>', { page, size });
				response = await ReviewService.getAllReviews({ page, size });
			}
			const payload = response?.data || response;
			// items 파싱: 배열 | {reviews} | {items} | {success,data}
			let items = Array.isArray(payload) ? payload : (payload?.reviews || payload?.items || payload?.data || []);
			if (!Array.isArray(items)) items = [];
			// total/page/size 보정 (meta 우선)
			const meta = payload?.meta;
			const totalFromServer = meta?.total ?? payload?.total;
			const total = Number.isFinite(Number(totalFromServer)) ? Number(totalFromServer) : items.length;
			const serverPage = (typeof meta?.page === 'number') ? meta.page : payload?.page; // 0-based
			const respPage = Number.isFinite(Number(serverPage)) ? Number(serverPage) + 1 : page; // UI 1-based
			const respSize = meta?.size ?? payload?.size ?? size;
			if (userRole === 'PARTNER') {
				console.log('[Partner Reviews] response meta =>', { page: serverPage ?? meta?.page, size: respSize, total: total, itemsCount: items.length });
			} else {
				console.log('[Admin Reviews] response meta =>', { page: serverPage ?? meta?.page, size: respSize, total: total, itemsCount: items.length });
			}
			setReviews(items);
			setTotalItems(Number(total) || 0);
			setCurrentPage(respPage);
			setPageSize(respSize);
		} catch (err) {
			console.error('리뷰 목록 조회 실패:', err);
			const errorMessage = err.response?.data?.message || err.message || '리뷰 목록을 불러오는데 실패했습니다.';
			message.error(errorMessage);
			setReviews([]);
			setTotalItems(0);
		} finally {
			setLoading(false);
		}
	};

	// 로컬 필터(현재 페이지 아이템에 제한적용)
	const locallyFiltered = React.useMemo(() => {
		let filtered = [...reviews];
		if (showUnansweredOnly) {
			filtered = filtered.filter(r => !r.reviewReply || r.reviewReply.trim() === '');
		}
		if (ratingFilter !== 'ALL') {
			const rating = parseInt(ratingFilter);
			filtered = filtered.filter(r => r.reviewRating === rating);
		}
		if (searchText) {
			const lower = searchText.toLowerCase();
			filtered = filtered.filter(r =>
				r.productName?.toLowerCase().includes(lower) ||
				r.customerName?.toLowerCase().includes(lower) ||
				r.reviewContent?.toLowerCase().includes(lower)
			);
		}
		return filtered;
	}, [reviews, ratingFilter, searchText, showUnansweredOnly]);

	const handleViewDetail = async (reviewNo) => {
		try {
			const response = await ReviewService.getReview(reviewNo);
			const reviewData = response.data || response;
			setSelectedReview(reviewData);
			setDetailModalVisible(true);
		} catch (err) {
			message.error(err.response?.data?.message || '리뷰 상세 정보를 불러오는데 실패했습니다.');
		}
	};

	const handleDeleteReview = async (reviewNo) => {
		try {
			await ReviewService.deleteReview(reviewNo);
			message.success('리뷰가 삭제되었습니다.');
			fetchAllReviews();
		} catch (err) {
			message.error(err.response?.data?.message || '리뷰 삭제에 실패했습니다.');
		}
	};

	const handleOpenReplyModal = async (reviewNo) => {
		try {
			const response = await ReviewService.getReview(reviewNo);
			const reviewData = response.data || response;
			setSelectedReview(reviewData);
			form.setFieldsValue({
				reviewReply: reviewData.reviewReply || ''
			});
			setReplyModalVisible(true);
		} catch (err) {
			message.error(err.response?.data?.message || '리뷰 정보를 불러오는데 실패했습니다.');
		}
	};

	const handleSubmitReply = async (values) => {
		try {
			if (selectedReview.reviewReply) {
				// 답변 수정
				await ReviewService.updateReviewReply(selectedReview.reviewNo, values.reviewReply);
				message.success('답변이 수정되었습니다.');
			} else {
				// 답변 작성
				await ReviewService.addReviewReply(selectedReview.reviewNo, values.reviewReply);
				message.success('답변이 작성되었습니다.');
			}
			setReplyModalVisible(false);
			form.resetFields();
			fetchAllReviews();
			if (detailModalVisible) {
				await handleViewDetail(selectedReview.reviewNo);
			}
		} catch (err) {
			message.error(err.response?.data?.message || '답변 작성/수정에 실패했습니다.');
		}
	};

	const handleDeleteReply = async () => {
		try {
			await ReviewService.deleteReviewReply(selectedReview.reviewNo);
			message.success('답변이 삭제되었습니다.');
			setReplyModalVisible(false);
			form.resetFields();
			fetchAllReviews();
			if (detailModalVisible) {
				await handleViewDetail(selectedReview.reviewNo);
			}
		} catch (err) {
			message.error(err.response?.data?.message || '답변 삭제에 실패했습니다.');
		}
	};

	const formatDate = (dateString) => {
		if (!dateString) return '-';
		const date = new Date(dateString);
		return date.toLocaleString('ko-KR', {
			year: 'numeric',
			month: '2-digit',
			day: '2-digit',
			hour: '2-digit',
			minute: '2-digit'
		});
	};

	const columns = [
		{
			title: '리뷰 번호',
			dataIndex: 'reviewNo',
			key: 'reviewNo',
			width: 100,
			sorter: (a, b) => a.reviewNo - b.reviewNo,
		},
		{
			title: '상품명',
			dataIndex: 'productName',
			key: 'productName',
			width: 200,
			render: (_, record) => (
				<div>
					<div style={{ fontWeight: 500 }}>{record.productName}</div>
					{record.color && record.size && (
						<div style={{ fontSize: 12, color: '#999' }}>
							{record.color} / {record.size}
						</div>
					)}
				</div>
			),
		},
		{
			title: '고객명',
			dataIndex: 'customerName',
			key: 'customerName',
			width: 120,
		},
		{
			title: '평점',
			dataIndex: 'reviewRating',
			key: 'reviewRating',
			width: 120,
			render: (rating) => (
				<Tag color={getRatingColor(rating)}>
					<StarOutlined /> {rating}점
				</Tag>
			),
			sorter: (a, b) => a.reviewRating - b.reviewRating,
		},
		{
			title: '작성일',
			dataIndex: 'reviewCreatedAt',
			key: 'reviewCreatedAt',
			width: 180,
			render: (date) => formatDate(date),
			sorter: (a, b) => new Date(a.reviewCreatedAt) - new Date(b.reviewCreatedAt),
		},
		{
			title: '주문 번호',
			dataIndex: 'orderNo',
			key: 'orderNo',
			width: 120,
		},
		{
			title: '작업',
			key: 'action',
			width: 200,
			fixed: 'right',
			render: (_, record) => (
				<Space>
					<Button
						type="link"
						icon={<EyeOutlined />}
						onClick={() => handleViewDetail(record.reviewNo)}
					>
						상세
					</Button>
					{/* 관리자만 리뷰 삭제 가능 */}
					{userRole === 'ADMIN' && (
						<Popconfirm
							title="리뷰 삭제"
							description="정말 이 리뷰를 삭제하시겠습니까?"
							onConfirm={() => handleDeleteReview(record.reviewNo)}
							okText="삭제"
							cancelText="취소"
						>
							<Button
								type="link"
								danger
								icon={<DeleteOutlined />}
							>
								삭제
							</Button>
						</Popconfirm>
					)}
				</Space>
			),
		},
	];

	// 통계 계산
	const totalReviews = totalItems;
	const avgRating = locallyFiltered.length > 0
		? (locallyFiltered.reduce((sum, r) => sum + r.reviewRating, 0) / locallyFiltered.length).toFixed(1)
		: 0;
	const ratingDistribution = {
		5: locallyFiltered.filter(r => r.reviewRating === 5).length,
		4: locallyFiltered.filter(r => r.reviewRating === 4).length,
		3: locallyFiltered.filter(r => r.reviewRating === 3).length,
		2: locallyFiltered.filter(r => r.reviewRating === 2).length,
		1: locallyFiltered.filter(r => r.reviewRating === 1).length,
	};
	
	// 답변 없는 새 리뷰 (파트너용)
	const unansweredReviews = reviews.filter(review => 
		!review.reviewReply || review.reviewReply.trim() === ''
	);
	const unansweredCount = unansweredReviews.length;

	const analysisProductMeta = React.useMemo(() => {
		const pn = selectedProductNos?.length === 1 ? selectedProductNos[0] : null;
		const prod = pn != null ? (products || []).find((p) => p?.productNo === pn) : null;
		const hasOpts = Array.isArray(prod?.options) && prod.options.length > 0;
		return { productNo: pn, product: prod, hasOptions: hasOpts };
	}, [products, selectedProductNos]);

	// 옵션 범위가 "전체"일 때: 상품의 모든 optionNo를 자동 선택 (수동 multi와 동일한 API 파라미터)
	useEffect(() => {
		if (userRole !== 'PARTNER') return;
		if (aiOptionScope !== 'whole') return;
		const prod = analysisProductMeta.product;
		if (!prod || !Array.isArray(prod.options) || prod.options.length === 0) return;
		const nos = prod.options.map((o) => o?.optionNo).filter((v) => v != null && v !== '');
		if (nos.length === 0) return;
		setSelectedOptionNos(nos);
	}, [userRole, aiOptionScope, analysisProductMeta.product]);

	const handleAnalyzeReviews = async (options = {}) => {
		const { isRetry = false } = options;
		if (userRole !== 'PARTNER') {
			message.warning('파트너 계정에서만 리뷰 AI 분석을 사용할 수 있습니다.');
			return;
		}
		try {
			setAnalysisLoading(true);
			setAnalysisErrorMessage('');
			if (!selectedProductNos || selectedProductNos.length !== 1) {
				message.warning('분석할 상품을 선택해주세요.');
				setAnalysisLoading(false);
				return;
			}
			const { hasOptions: apHasOptions } = analysisProductMeta;
			if (apHasOptions && aiOptionScope === 'one' && selectedOptionNos.length !== 1) {
				message.warning('분석할 옵션을 한 가지 선택해주세요.');
				setAnalysisLoading(false);
				return;
			}
			if (apHasOptions && aiOptionScope === 'some' && selectedOptionNos.length < 1) {
				message.warning('분석에 포함할 옵션을 한 개 이상 선택해주세요.');
				setAnalysisLoading(false);
				return;
			}
			if (apHasOptions && aiOptionScope === 'whole' && selectedOptionNos.length < 1) {
				message.warning('옵션 목록을 불러오는 중입니다. 잠시 후 다시 시도해주세요.');
				setAnalysisLoading(false);
				return;
			}
			if (analysisDateMode === 'range' && !(Array.isArray(dateRange) && dateRange.length === 2 && dateRange[0] && dateRange[1])) {
				message.warning('기간 지정 모드에서는 시작일과 종료일을 선택해주세요.');
				setAnalysisLoading(false);
				return;
			}

			const productNo = selectedProductNos[0];
			const cparams = { productNo };
			if (apHasOptions && selectedOptionNos?.length > 0) {
				cparams.optionNos = selectedOptionNos;
			}
			if (analysisDateMode === 'range' && Array.isArray(dateRange) && dateRange.length === 2 && dateRange[0] && dateRange[1]) {
				cparams.fromAt = new Date(dateRange[0].toDate()).toISOString().slice(0, 19);
				cparams.toAt = new Date(dateRange[1].toDate()).toISOString().slice(0, 19);
			}

			const candResp = await ReviewService.getPartnerReviewAiCandidates(cparams);
			if (candResp?.success === false) {
				const msg = candResp?.message || '분석 대상 리뷰를 불러오지 못했습니다.';
				message.error(msg);
				setAnalysisLoading(false);
				return;
			}
			const candPayload = candResp?.data ?? candResp;
			const sourceReviews = Array.isArray(candPayload?.reviews) ? candPayload.reviews : [];
			const meta = candPayload?.meta || {};
			if (Number.isFinite(Number(meta.recentCountDefault))) {
				setAiSampleMeta({
					recentCountDefault: Number(meta.recentCountDefault),
					minimumRequired: Number.isFinite(Number(meta.minimumRequired))
						? Number(meta.minimumRequired)
						: MIN_ANALYSIS_REVIEWS,
					hardCap: Number.isFinite(Number(meta.hardCap)) ? Number(meta.hardCap) : 10,
					requestedLimit: Number.isFinite(Number(meta.requestedLimit))
						? Number(meta.requestedLimit)
						: Number(meta.recentCountDefault) || 3
				});
			}
			const minimumRequired = Number.isFinite(Number(meta.minimumRequired))
				? Number(meta.minimumRequired)
				: MIN_ANALYSIS_REVIEWS;

			if (sourceReviews.length < minimumRequired) {
				message.warning(`리뷰 AI 분석은 최소 ${minimumRequired}건 이상 필요합니다. (현재 ${sourceReviews.length}건)`);
				setAnalysisLoading(false);
				return;
			}

			const cap = Number.isFinite(Number(meta.requestedLimit))
				? Number(meta.requestedLimit)
				: sourceReviews.length;

			// 날짜 범위: UI 선택이 있으면 우선 적용, 없으면 리뷰 데이터 기준 파생
			let uiFromAt = null, uiToAt = null;
			if (analysisDateMode === 'range' && Array.isArray(dateRange) && dateRange.length === 2 && dateRange[0] && dateRange[1]) {
				uiFromAt = dateRange[0].toDate();
				uiToAt = dateRange[1].toDate();
			} else {
				const reviewDates = sourceReviews
					.map((r) => r?.reviewCreatedAt)
					.filter(Boolean)
					.map((d) => new Date(d))
					.filter((d) => !Number.isNaN(d.getTime()))
					.sort((a, b) => a.getTime() - b.getTime());
				uiFromAt = reviewDates.length > 0 ? reviewDates[0] : null;
				uiToAt = reviewDates.length > 0 ? reviewDates[reviewDates.length - 1] : null;
			}

			const productNos = selectedProductNos;

			const payload = {
				// partnerId는 백엔드에서 세션으로 강제 주입되며, 바디로 보내도 무시됩니다.
				fromAt: uiFromAt ? new Date(uiFromAt).toISOString().slice(0, 19) : null,
				toAt: uiToAt ? new Date(uiToAt).toISOString().slice(0, 19) : null,
				productNos,
				maxReviews: cap,
				reviews: sourceReviews.map((r) => ({
					reviewNo: r?.reviewNo,
					productNo: r?.productNo,
					optionNo: r?.optionNo ?? null,
					rating: r?.reviewRating,
					content: r?.reviewContent || '',
					createdAt: r?.reviewCreatedAt ? new Date(r.reviewCreatedAt).toISOString().slice(0, 19) : null
				}))
			};

			const res = await PartnerService.postPartnerReviewAnalysis(payload);
			if (res?.success === false) {
				setAnalysisResult(null);
				const msg = res?.message || '리뷰 AI 분석 요청에 실패했습니다.';
				setAnalysisErrorMessage(msg);
				message.error(msg);
				return;
			}
			const bodyData = res?.data ?? null;
			if (bodyData == null) {
				setAnalysisResult(null);
				const msg = res?.message || '분석 결과가 비어 있습니다.';
				setAnalysisErrorMessage(msg);
				message.warning(msg);
				return;
			}
			setAnalysisResult(bodyData);
			setAnalysisErrorMessage('');
			if (isRetry) {
				message.success('리뷰 AI 분석을 다시 시도했습니다.');
			}
		} catch (err) {
			setAnalysisResult(null);
			const msg = err?.response?.data?.message || '리뷰 AI 분석 호출 실패';
			const code = err?.response?.data?.code;
			setAnalysisErrorMessage(msg);
			if (code === 'REVIEW_ANALYSIS_NOT_ENOUGH_REVIEWS') {
				message.warning(msg);
			} else {
				message.error(msg);
			}
		} finally {
			setAnalysisLoading(false);
		}
	};

	const isFallbackResult = (result) => {
		if (!result) return false;
		const summary = String(result?.summary || '').trim();
		const policyTypes = Array.isArray(result?.alerts?.policyTypes) ? result.alerts.policyTypes : [];
		return summary === '리뷰 분석 일시 불가' || summary.includes('리뷰 분석 일시 불가') || policyTypes.includes('AI_UNAVAILABLE');
	};

	const toSeverityLabel = (severity) => {
		const key = String(severity || '').toUpperCase();
		if (key === 'HIGH') return { text: '높음', color: 'red' };
		if (key === 'MEDIUM') return { text: '보통', color: 'orange' };
		if (key === 'LOW') return { text: '낮음', color: 'green' };
		return { text: key || '-', color: 'default' };
	};

	const toIssueTypeLabel = (type) => {
		const key = String(type || '').toUpperCase();
		const map = {
			SIZE_INCONSISTENCY: '사이즈 편차',
			DELIVERY_DELAY: '배송 지연',
			QUALITY_ISSUE: '품질 이슈',
			PACKAGING_ISSUE: '포장 이슈',
			CS_ISSUE: '고객응대 이슈'
		};
		return map[key] || key || '이슈';
	};

	/** 백엔드 ReviewAnalysisService 허용 area 값과 동일 */
	const toAreaLabel = (area) => {
		const key = String(area || '').toUpperCase();
		const map = {
			DETAIL: '상품 상세',
			SIZE_GUIDE: '사이즈 가이드',
			IMAGES: '상품 이미지',
			PACKAGING: '포장·배송 패키지',
			FAQ: 'FAQ',
			CS_MACRO: '고객 안내·상담 문구',
			INVENTORY: '재고·품절 안내',
			// 구버전/모델 변형 대비
			PRODUCT_DETAIL: '상품 상세',
			CS_TEMPLATE: '고객 안내·상담 문구',
			LOGISTICS: '물류·배송',
			PRICING: '가격 정책',
			QUALITY: '품질 개선'
		};
		return map[key] || (key ? `${key} 영역` : '운영');
	};

	const listToolbarAndTable = (
		<>
			<Row gutter={16} style={{ marginBottom: 24 }}>
				<Col span={6}>
					<Card>
						<Statistic
							title="전체 리뷰"
							value={totalReviews}
							prefix={<CommentOutlined />}
						/>
					</Card>
				</Col>
				<Col span={6}>
					<Card>
						<Statistic
							title="평균 평점"
							value={avgRating}
							prefix={<StarOutlined />}
							suffix="점"
							precision={1}
						/>
					</Card>
				</Col>
				<Col span={6}>
					<Card>
						<Statistic
							title="5점 리뷰"
							value={ratingDistribution[5]}
							valueStyle={{ color: '#3f8600' }}
						/>
					</Card>
				</Col>
				<Col span={6}>
					<Card>
						<Statistic
							title="1-2점 리뷰"
							value={ratingDistribution[1] + ratingDistribution[2]}
							valueStyle={{ color: '#cf1322' }}
						/>
					</Card>
				</Col>
			</Row>

			<Row gutter={16} style={{ marginBottom: 16 }}>
				<Col span={12}>
					<Search
						placeholder="상품명, 고객명, 리뷰 내용으로 검색"
						allowClear
						onChange={(e) => setSearchText(e.target.value)}
						style={{ width: '100%' }}
					/>
				</Col>
				<Col span={6}>
					<Select
						value={ratingFilter}
						onChange={setRatingFilter}
						style={{ width: '100%' }}
						placeholder="평점 필터"
					>
						<Option value="ALL">전체 평점</Option>
						<Option value="5">5점</Option>
						<Option value="4">4점</Option>
						<Option value="3">3점</Option>
						<Option value="2">2점</Option>
						<Option value="1">1점</Option>
					</Select>
				</Col>
				<Col span={6}>
					<Button onClick={() => fetchAllReviews(currentPage, pageSize)}>새로고침</Button>
				</Col>
			</Row>

			<Table
				columns={columns}
				dataSource={locallyFiltered}
				rowKey="reviewNo"
				loading={loading}
				scroll={{ x: 1200 }}
				pagination={{
					current: currentPage,
					pageSize: pageSize,
					total: totalItems,
					showSizeChanger: true,
					showTotal: (total) => `총 ${total}개`,
					onChange: (p, s) => {
						setCurrentPage(p);
						setPageSize(s);
					}
				}}
			/>
		</>
	);

	const aiAnalysisPanel = (
		<Card
			size="small"
			title={<Space>리뷰 AI 분석</Space>}
			extra={
				(() => {
					const productOk = analysisProductMeta.productNo != null;
					const optionOk = !analysisProductMeta.hasOptions
						? true
						: aiOptionScope === 'whole'
							? selectedOptionNos.length >= 1
							: aiOptionScope === 'one'
								? selectedOptionNos.length === 1
								: selectedOptionNos.length >= 1;
					const dateOk = analysisDateMode !== 'range'
						? true
						: Array.isArray(dateRange) && dateRange.length === 2 && dateRange[0] && dateRange[1];
					const readyOk = productOk && optionOk && dateOk;
					const disabledReason = !productOk
						? '상품을 선택해주세요'
						: !optionOk
							? (aiOptionScope === 'one'
								? '옵션을 한 가지 선택해주세요'
								: aiOptionScope === 'some'
									? '옵션을 한 개 이상 선택해주세요'
									: '옵션을 불러오는 중입니다')
							: !dateOk
								? '시작일과 종료일을 선택해주세요'
							: null;
					const buttonTitle = disabledReason || (
						analysisDateMode === 'range'
							? `선택한 기간 안에서 최대 ${aiSampleMeta.requestedLimit || aiSampleMeta.recentCountDefault}건을 불러와 분석합니다.`
							: `선택 상품 기준 최신 ${aiSampleMeta.requestedLimit || aiSampleMeta.recentCountDefault}건(서버 설정)을 불러와 분석합니다.`
					);
					return (
						<Space>
							<Button
								type="primary"
								loading={analysisLoading}
								onClick={() => handleAnalyzeReviews()}
								disabled={analysisLoading || !readyOk}
								title={buttonTitle}
							>
								조회 ({analysisDateMode === 'range' ? '기간 기준' : '최신 기준'})
							</Button>
							{isFallbackResult(analysisResult) && (
								<Button
									loading={analysisLoading}
									onClick={() => handleAnalyzeReviews({ isRetry: true })}
									disabled={analysisLoading}
								>
									다시 시도
								</Button>
							)}
						</Space>
					);
				})()
			}
			style={{ marginBottom: 0 }}
		>
			<Spin spinning={analysisLoading}>
				<Space direction="vertical" size={12} style={{ width: '100%', marginBottom: 8 }}>
					<Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
						기본은 <strong>최신 순</strong>입니다. 기간 선택도 가능합니다.
					</Typography.Paragraph>
					<div>
						<div style={{ fontWeight: 600, marginBottom: 6 }}>① 분석할 상품</div>
						<Select
							allowClear
							style={{ width: '100%', maxWidth: 560 }}
							value={selectedProductNos[0]}
							onChange={(val) => {
								setSelectedProductNos(val ? [val] : []);
								setSelectedOptionNos([]);
								setAiOptionScope('whole');
							}}
							placeholder="상품을 선택하세요"
							maxTagCount="responsive"
						>
							{(products || []).map((p) => (
								<Option key={p?.productNo} value={p?.productNo}>
									{p?.productName || `상품 #${p?.productNo}`}
								</Option>
							))}
						</Select>
					</div>
					{analysisProductMeta.productNo && analysisProductMeta.hasOptions ? (
						<div>
							<div style={{ fontWeight: 600, marginBottom: 6 }}>② 옵션 범위</div>
							<Segmented
								options={[
									{ label: '전체', value: 'whole' },
									{ label: '단일', value: 'one' },
									{ label: '복수', value: 'some' },
								]}
								value={aiOptionScope}
								onChange={(v) => {
									setAiOptionScope(v);
									if (v === 'one' || v === 'some') setSelectedOptionNos([]);
								}}
							/>
							
							{aiOptionScope === 'one' && (
								<>
									<div style={{ fontWeight: 600, marginTop: 10, marginBottom: 6 }}>분석할 옵션</div>
									<Select
										allowClear
										style={{ width: '100%', maxWidth: 560 }}
										value={selectedOptionNos[0]}
										onChange={(val) => setSelectedOptionNos(val != null ? [val] : [])}
										placeholder="옵션 한 가지를 선택하세요"
										disabled={!analysisProductMeta.productNo}
									>
										{(Array.isArray(analysisProductMeta.product?.options) ? analysisProductMeta.product.options : []).map((opt) => (
											<Option key={opt?.optionNo} value={opt?.optionNo}>
												{renderOptionLabel(opt)}
											</Option>
										))}
									</Select>
								</>
							)}
							{aiOptionScope === 'some' && (
								<>
									<div style={{ fontWeight: 600, marginTop: 10, marginBottom: 6 }}>포함할 옵션 (복수)</div>
									<Select
										mode="multiple"
										allowClear
										style={{ width: '100%', maxWidth: 560 }}
										value={selectedOptionNos}
										onChange={(vals) => setSelectedOptionNos(vals)}
										placeholder="옵션을 한 개 이상 선택하세요"
										disabled={!analysisProductMeta.productNo}
										maxTagCount="responsive"
									>
										{(Array.isArray(analysisProductMeta.product?.options) ? analysisProductMeta.product.options : []).map((opt) => (
											<Option key={opt?.optionNo} value={opt?.optionNo}>
												{renderOptionLabel(opt)}
											</Option>
										))}
									</Select>
								</>
							)}
						</div>
					) : analysisProductMeta.productNo && !analysisProductMeta.hasOptions ? (
						<Alert type="info" showIcon message="옵션이 없는 상품입니다. 이 상품의 리뷰만 대상으로 합니다." />
					) : (
						<div style={{ color: '#888' }}>상품을 먼저 선택하세요.</div>
					)}
					<div
						style={{
							background: '#fafafa',
							border: '1px solid #f0f0f0',
							borderRadius: 10,
							padding: 14
						}}
					>
						<div style={{ fontWeight: 600, marginBottom: 8 }}>③ 리뷰 범위</div>
						<Space direction="vertical" size={10} style={{ width: '100%' }}>
							<Segmented
								options={[
									{ label: '최신', value: 'latest' },
									{ label: '기간 지정', value: 'range' },
								]}
								value={analysisDateMode}
								onChange={(v) => setAnalysisDateMode(v)}
							/>
							{analysisDateMode === 'latest' ? (
								<Alert
									type="info"
									showIcon
									icon={<CalendarOutlined />}
									message={`최신 리뷰만 ${aiSampleMeta.recentCountDefault}건 분석합니다.`}
								/>
							) : (
								<Space direction="vertical" size={10} style={{ width: '100%' }}>
									<Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
										선택한 범위 안에서 리뷰를 분석합니다다.
									</Typography.Paragraph>
									<RangePicker
										style={{ width: '100%', maxWidth: 400 }}
										value={dateRange}
										onChange={(vals) => setDateRange(vals || [])}
										placeholder={['시작일', '종료일']}
									/>
									<Tag color={Array.isArray(dateRange) && dateRange.length === 2 && dateRange[0] && dateRange[1] ? 'blue' : 'default'} style={{ width: 'fit-content' }}>
										{Array.isArray(dateRange) && dateRange.length === 2 && dateRange[0] && dateRange[1]
											? `${dateRange[0].format('YYYY-MM-DD')} ~ ${dateRange[1].format('YYYY-MM-DD')}`
											: '기간을 선택해주세요'}
									</Tag>
								</Space>
							)}
						</Space>
					</div>
				</Space>
				{analysisResult ? (
					<Space direction="vertical" size={8} style={{ width: '100%' }}>
						{isFallbackResult(analysisResult) ? (
							<>
								<Tag color="red">AI 분석 일시 불가</Tag>
								<div style={{ color: '#cf1322', fontWeight: 600 }}>
									일시적인 AI 응답 문제가 발생했습니다. 잠시 후 재시도해주세요.
								</div>
								<div>
									<strong>요약</strong>
									<div>{analysisResult?.summary || '-'}</div>
								</div>
								<Card size="small" title="안내">
									<div style={{ color: '#666' }}>
										{Array.isArray(analysisResult?.actions) && analysisResult.actions.length > 0
											? (analysisResult.actions[0]?.recommendation || '잠시 후 다시 시도해주세요.')
											: '잠시 후 다시 시도해주세요.'}
									</div>
								</Card>
							</>
						) : (
							<>
								<div>
									<strong>요약</strong>
									<div>{analysisResult?.summary || '-'}</div>
								</div>
								<Row gutter={[16, 8]}>
									<Col xs={24} md={12}>
										<Card size="small" title="주요 이슈">
											{Array.isArray(analysisResult?.issues) && analysisResult.issues.length > 0 ? (
												analysisResult.issues.map((issue, idx) => (
													<div key={`issue-${idx}`} style={{ marginBottom: 10, paddingBottom: 8, borderBottom: '1px dashed #f0f0f0' }}>
														<div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
															<strong>{issue?.title || toIssueTypeLabel(issue?.type)}</strong>
															<Tag color={toSeverityLabel(issue?.severity).color}>{toSeverityLabel(issue?.severity).text}</Tag>
														</div>
														<div style={{ color: '#666' }}>발생 빈도: {issue?.frequency ?? '-'}회</div>
													</div>
												))
											) : (
												<div>-</div>
											)}
										</Card>
									</Col>
									<Col xs={24} md={12}>
										<Card
											size="small"
											title="AI 권장 조치"
											extra={(
												<Tooltip title="선택한 리뷰 샘플을 읽고, 파트너 운영에 참고할 만한 개선 아이디어를 제안한 것입니다. 실행 여부는 직접 판단해 주세요.">
													<span style={{ color: '#999', cursor: 'help', fontSize: 12 }}>?</span>
												</Tooltip>
											)}
										>
											<Typography.Paragraph type="secondary" style={{ marginBottom: 12, fontSize: 13 }}>
												위 줄은 <strong>어느 영역</strong>을 손보면 좋을지, 그 아래는 <strong>무엇을 하면 좋을지</strong>입니다.
											</Typography.Paragraph>
											{Array.isArray(analysisResult?.actions) && analysisResult.actions.length > 0 ? (
												analysisResult.actions.map((action, idx) => (
													<div key={`action-${idx}`} style={{ marginBottom: 12, paddingBottom: 10, borderBottom: '1px dashed #f0f0f0' }}>
														<div style={{ marginBottom: 6, fontWeight: 600 }}>{toAreaLabel(action?.area)}</div>
														<div style={{ lineHeight: 1.6 }}>{action?.recommendation || action?.title || action?.action || action?.description || '-'}</div>
													</div>
												))
											) : (
												<div>-</div>
											)}
										</Card>
									</Col>
								</Row>
							</>
						)}
					</Space>
				) : (
					<div style={{ color: '#888' }}>
						기본은 <strong>최신 {aiSampleMeta.recentCountDefault}건</strong>입니다. {aiSampleMeta.minimumRequired || MIN_ANALYSIS_REVIEWS}건 미만이면 분석할 수 없습니다.
					</div>
				)}
			</Spin>
		</Card>
	);

	return (
		<div>
			<Card>
				{userRole === 'PARTNER' ? (
					<Tabs
						activeKey={reviewTabKey}
						onChange={setReviewTabKey}
						items={[
							{
								key: 'list',
								label: (
									<span>
										<CommentOutlined /> 리뷰 관리
									</span>
								),
								children: (
									<>
										{unansweredCount > 0 && (
											<Card
												style={{
													marginBottom: 24,
													background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
													border: 'none',
													color: 'white'
												}}
											>
												<Row align="middle" justify="space-between">
													<Col>
														<Space size="large">
															<CommentOutlined style={{ fontSize: 24 }} />
															<div>
																<div style={{ fontSize: 18, fontWeight: 'bold', marginBottom: 4 }}>
																	답변 대기 리뷰가 {unansweredCount}건 있습니다
																</div>
																<div style={{ fontSize: 14, opacity: 0.9 }}>
																	새로운 리뷰에 답변을 작성해주세요
																</div>
															</div>
														</Space>
													</Col>
													<Col>
														<Button
															type="primary"
															size="large"
															icon={<CommentOutlined />}
															onClick={() => {
																if (showUnansweredOnly) {
																	setShowUnansweredOnly(false);
																} else {
																	setShowUnansweredOnly(true);
																	setSearchText('');
																	setRatingFilter('ALL');
																}
															}}
															style={{
																background: showUnansweredOnly ? '#52c41a' : 'white',
																color: showUnansweredOnly ? 'white' : '#667eea',
																border: 'none',
																fontWeight: 'bold'
															}}
														>
															{showUnansweredOnly ? '전체 보기' : '답변 대기 보기'}
														</Button>
													</Col>
												</Row>
											</Card>
										)}
										{listToolbarAndTable}
									</>
								),
							},
							{
								key: 'ai',
								label: (
									<span>
										<ThunderboltOutlined /> AI 분석
									</span>
								),
								children: aiAnalysisPanel,
							},
						]}
					/>
				) : (
					listToolbarAndTable
				)}
			</Card>

			{/* 리뷰 상세 모달 */}
			<Modal
				title="리뷰 상세 정보"
				open={detailModalVisible}
				onCancel={() => {
					setDetailModalVisible(false);
					setSelectedReview(null);
				}}
				footer={[
					// 파트너만 답변 작성/수정 가능
					userRole === 'PARTNER' && (
						<Button key="reply" onClick={() => {
							setDetailModalVisible(false);
							handleOpenReplyModal(selectedReview.reviewNo);
						}}>
							{selectedReview?.reviewReply ? '답변 수정' : '답변 작성'}
						</Button>
					),
					<Button key="close" onClick={() => {
						setDetailModalVisible(false);
						setSelectedReview(null);
					}}>
						닫기
					</Button>
				].filter(Boolean)}
				width={800}
			>
				{selectedReview && (
					<Descriptions bordered column={1}>
						<Descriptions.Item label="리뷰 번호">
							{selectedReview.reviewNo}
						</Descriptions.Item>
						<Descriptions.Item label="상품 정보">
							<Space>
								<Image
									src={selectedReview.productImageUrl || '/img/LetsDiveIn03.png'}
									alt={selectedReview.productName}
									width={80}
									height={80}
									style={{ objectFit: 'cover', borderRadius: 4 }}
									fallback="/img/LetsDiveIn03.png"
								/>
								<div>
									<div style={{ fontWeight: 500, fontSize: 16 }}>
										{selectedReview.productName}
									</div>
									{selectedReview.color && selectedReview.size && (
										<div style={{ fontSize: 14, color: '#999', marginTop: 4 }}>
											{selectedReview.color} / {selectedReview.size}
										</div>
									)}
									<div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>
										상품 번호: {selectedReview.productNo}
									</div>
								</div>
							</Space>
						</Descriptions.Item>
						<Descriptions.Item label="고객 정보">
							<Space>
								<UserOutlined />
								<span>{selectedReview.customerName}</span>
								<span style={{ color: '#999' }}>(ID: {selectedReview.customerId})</span>
							</Space>
						</Descriptions.Item>
						<Descriptions.Item label="평점">
							<Space>
								<Rate disabled value={selectedReview.reviewRating} />
								<Tag color={getRatingColor(selectedReview.reviewRating)}>
									{selectedReview.reviewRating}점
								</Tag>
							</Space>
						</Descriptions.Item>
						<Descriptions.Item label="리뷰 내용">
							<div style={{ whiteSpace: 'pre-wrap', minHeight: 100 }}>
								{selectedReview.reviewContent}
							</div>
						</Descriptions.Item>
						<Descriptions.Item label="주문 정보">
							<Space>
								<ShoppingCartOutlined />
								<span>주문 번호: {selectedReview.orderNo}</span>
								<span style={{ color: '#999' }}>
									(주문 상품 번호: {selectedReview.orderItemNo})
								</span>
							</Space>
						</Descriptions.Item>
						<Descriptions.Item label="작성일">
							{formatDate(selectedReview.reviewCreatedAt)}
						</Descriptions.Item>
						{selectedReview.reviewReply && (
							<Descriptions.Item label="답변">
								<div style={{ whiteSpace: 'pre-wrap', backgroundColor: '#f5f5f5', padding: 12, borderRadius: 4 }}>
									{selectedReview.reviewReply}
								</div>
								{selectedReview.reviewReplyCreatedAt && (
									<div style={{ fontSize: 12, color: '#999', marginTop: 8 }}>
										답변 작성일: {formatDate(selectedReview.reviewReplyCreatedAt)}
									</div>
								)}
							</Descriptions.Item>
						)}
					</Descriptions>
				)}
			</Modal>

			{/* 리뷰 답변 작성/수정 모달 */}
			<Modal
				title={selectedReview?.reviewReply ? '리뷰 답변 수정' : '리뷰 답변 작성'}
				open={replyModalVisible}
				onCancel={() => {
					setReplyModalVisible(false);
					form.resetFields();
				}}
				onOk={() => form.submit()}
				okText={selectedReview?.reviewReply ? '수정' : '작성'}
				cancelText="취소"
				width={600}
			>
				<Form
					form={form}
					layout="vertical"
					onFinish={handleSubmitReply}
				>
					<Form.Item
						label="답변 내용"
						name="reviewReply"
						rules={[{ required: true, message: '답변 내용을 입력해주세요.' }]}
					>
						<TextArea
							rows={6}
							placeholder="리뷰에 대한 답변을 작성해주세요."
							maxLength={1000}
							showCount
						/>
					</Form.Item>
					{selectedReview?.reviewReply && (
						<Form.Item>
							<Popconfirm
								title="답변 삭제"
								description="정말 이 답변을 삭제하시겠습니까?"
								onConfirm={handleDeleteReply}
								okText="삭제"
								cancelText="취소"
							>
								<Button danger icon={<DeleteOutlined />}>
									답변 삭제
								</Button>
							</Popconfirm>
						</Form.Item>
					)}
				</Form>
			</Modal>
		</div>
	);
};

export default ReviewManagement;
