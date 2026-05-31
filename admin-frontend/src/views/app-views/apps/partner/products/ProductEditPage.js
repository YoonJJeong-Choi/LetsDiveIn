import React, { useEffect, useMemo } from 'react';
import { Alert, Button, Card, message, Spin } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate, useParams } from 'react-router-dom';
import PageHeaderAlt from 'components/layout-components/PageHeaderAlt';
import Flex from 'components/shared-components/Flex';
import { fetchMyProducts, updateProduct } from 'store/slices/partnerSlice';
import ProductEditModal from './ProductEditModal';

const getUpdateSuccessMessage = (currentStatus, result) => {
	if (currentStatus === 'REJECTED') {
		return '상품이 수정되었고 재신청되었습니다. 관리자 승인 후 판매가 시작됩니다.';
	}
	if (currentStatus === 'PENDING') {
		return '상품이 수정되었습니다. 관리자 승인 후 판매가 시작됩니다.';
	}
	if (currentStatus === 'ACTIVE') {
		return result?.productActiveStatus === 'PENDING_UPDATE'
			? '상품이 수정되었습니다. 중요한 항목 변경으로 인해 관리자 승인이 필요합니다.'
			: '상품이 수정되었습니다. 변경 사항이 즉시 반영되었습니다.';
	}
	if (currentStatus === 'PENDING_UPDATE') {
		return '상품 수정 내용이 업데이트되었습니다. 관리자 승인 후 변경 사항이 반영됩니다.';
	}
	if (currentStatus === 'INACTIVE') {
		return ['PENDING', 'PENDING_UPDATE'].includes(result?.productActiveStatus)
			? '상품이 수정되었습니다. 관리자 승인 후 재활성화됩니다.'
			: '상품이 수정되었습니다.';
	}
	return '상품이 수정되었습니다.';
};

const ProductEditPage = () => {
	const dispatch = useDispatch();
	const navigate = useNavigate();
	const { productNo } = useParams();
	const { products, loading, error } = useSelector((state) => state.partner);

	useEffect(() => {
		dispatch(fetchMyProducts());
	}, [dispatch]);

	const product = useMemo(
		() => (products || []).find((item) => String(item.productNo) === String(productNo)),
		[products, productNo]
	);

	const goBack = () => navigate('/app/partner/products');

	const handleUpdateProduct = async (productData) => {
		try {
			const result = await dispatch(updateProduct({ productNo: product.productNo, productData })).unwrap();
			message.success(getUpdateSuccessMessage(product.productActiveStatus, result));
			goBack();
		} catch (err) {
			message.error(err || '상품 수정에 실패했습니다.');
		}
	};

	return (
		<>
			<PageHeaderAlt className="border-bottom">
				<div className="container">
					<Flex className="py-2" mobileFlex={false} justifyContent="space-between" alignItems="center">
						<div>
							<h2 className="mb-1">상품 수정</h2>
							<p className="mb-0 text-muted">판매 정보, 이미지, 옵션을 확인하고 필요한 항목을 수정합니다.</p>
						</div>
						<Button icon={<ArrowLeftOutlined />} onClick={goBack}>
							목록으로
						</Button>
					</Flex>
				</div>
			</PageHeaderAlt>
			<div className="container mt-4">
				{error && <Alert className="mb-3" type="error" message="오류" description={error} showIcon />}
				<Spin spinning={loading && !product}>
					{product ? (
						<ProductEditModal
							visible
							embedded
							onCancel={goBack}
							onSubmit={handleUpdateProduct}
							loading={loading}
							product={product}
						/>
					) : (
						<Card>
							<Alert
								type="warning"
								message="상품을 찾을 수 없습니다."
								description="목록을 새로고침한 뒤 다시 시도해주세요."
								showIcon
							/>
						</Card>
					)}
				</Spin>
			</div>
		</>
	);
};

export default ProductEditPage;
