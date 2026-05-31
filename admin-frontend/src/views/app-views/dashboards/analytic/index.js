import React from 'react';
import { useSelector } from 'react-redux';
import { Card, Result } from 'antd';
import AdminAnalyticDashboard from './AdminAnalyticDashboard';
import PartnerAnalyticDashboard from './PartnerAnalyticDashboard';

const AnalyticDashboard = () => {
	const role = useSelector((state) => state.auth?.user?.role);

	if (role === 'ADMIN') {
		return <AdminAnalyticDashboard />;
	}

	if (role === 'PARTNER') {
		return <PartnerAnalyticDashboard />;
	}

	return (
		<Card>
			<Result status="403" title="접근 제한" subTitle="관리자 또는 파트너 계정으로 로그인하세요." />
		</Card>
	);
};

export default AnalyticDashboard;
