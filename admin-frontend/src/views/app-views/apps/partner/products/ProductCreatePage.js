import React from 'react';
import { Button, message } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import PageHeaderAlt from 'components/layout-components/PageHeaderAlt';
import Flex from 'components/shared-components/Flex';
import { createProduct } from 'store/slices/partnerSlice';
import ProductCreateModal from './ProductCreateModal';

const ProductCreatePage = () => {
	const dispatch = useDispatch();
	const navigate = useNavigate();
	const { loading } = useSelector((state) => state.partner);

	const goBack = () => navigate('/app/partner/products');

	const handleCreateProduct = async (productData) => {
		try {
			await dispatch(createProduct(productData)).unwrap();
			message.success('상품 등록 신청이 완료되었습니다. 관리자 승인 후 판매가 시작됩니다.');
			goBack();
		} catch (err) {
			message.error(err || '상품 등록 신청에 실패했습니다.');
		}
	};

	return (
		<>
			<PageHeaderAlt className="border-bottom">
				<div className="container">
					<Flex className="py-2" mobileFlex={false} justifyContent="space-between" alignItems="center">
						<div>
							<h2 className="mb-1">상품 등록 신청</h2>
							<p className="mb-0 text-muted">상품 정보와 옵션을 입력하면 관리자 승인 요청으로 접수됩니다.</p>
						</div>
						<Button icon={<ArrowLeftOutlined />} onClick={goBack}>
							목록으로
						</Button>
					</Flex>
				</div>
			</PageHeaderAlt>
			<div className="container mt-4">
				<ProductCreateModal
					visible
					embedded
					onCancel={goBack}
					onSubmit={handleCreateProduct}
					loading={loading}
				/>
			</div>
		</>
	);
};

export default ProductCreatePage;
