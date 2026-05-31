import React, { useEffect } from 'react';
import { connect } from 'react-redux';
import { Button, Form, Input, Alert } from 'antd';
import { MailOutlined, LockOutlined } from '@ant-design/icons';
import PropTypes from 'prop-types';
import { 
	signIn, 
	showLoading, 
	showAuthMessage, 
	hideAuthMessage, 
} from 'store/slices/authSlice';
import { useLocation, useNavigate } from 'react-router-dom'
import { motion } from "framer-motion"

export const LoginForm = props => {
	
	const navigate = useNavigate();
	const location = useLocation();

	const { 
		showForgetPassword, 
		hideAuthMessage,
		onForgetPasswordClick,
		showLoading,
		extra, 
		signIn, 
		token, 
		loading,
		redirect,
		showMessage,
		message,
		allowRedirect = true
	} = props

	const authReason = new URLSearchParams(location.search).get('reason');
	const portalNotice = authReason === 'portal'
		? {
			type: 'warning',
			message: '현재 고객 계정으로 로그인되어 있습니다.',
			description: '관리자 페이지는 관리자 또는 파트너 계정만 이용할 수 있습니다. 해당 계정으로 다시 로그인해주세요.'
		}
		: authReason === 'expired'
			? {
				type: 'info',
				message: '로그인이 만료되었거나 해제되었습니다.',
				description: '관리자 또는 파트너 계정으로 다시 로그인해주세요.'
			}
			: null;

	const initialCredential = {
		email: 'user1@themenate.net',
		password: '2005ipo'
	}

	const onLogin = values => {
		showLoading()
		signIn(values);
	};

	useEffect(() => {
		if (token !== null && allowRedirect) {
			navigate(redirect)
		}
	}, [token, redirect, allowRedirect, navigate]);

	useEffect(() => {
		if (showMessage) {
			const timer = setTimeout(() => hideAuthMessage(), 3000)
			return () => {
				clearTimeout(timer);
			};
		}
	}, [showMessage, hideAuthMessage]);

	return (
		<>
			{
				portalNotice && (
					<Alert
						type={portalNotice.type}
						showIcon
						className="mb-3"
						message={portalNotice.message}
						description={portalNotice.description}
					/>
				)
			}
			<motion.div 
				initial={{ opacity: 0, marginBottom: 0 }} 
				animate={{ 
					opacity: showMessage ? 1 : 0,
					marginBottom: showMessage ? 20 : 0 
				}}> 
				<Alert type="error" showIcon message={message}></Alert>
			</motion.div>
			<Form 
				layout="vertical" 
				name="login-form" 
				initialValues={initialCredential}
				onFinish={onLogin}
			>
				<Form.Item 
					name="email" 
					label="이메일" 
					rules={[
						{ 
							required: true,
							message: '이메일을 입력해 주세요.',
						},
						{ 
							type: 'email',
							message: '올바른 이메일 형식을 입력해 주세요.'
						}
					]}>
					<Input prefix={<MailOutlined className="text-primary" />}/>
				</Form.Item>
				<Form.Item 
					name="password" 
					label={
						<div className={`${showForgetPassword? 'd-flex justify-content-between w-100 align-items-center' : ''}`}>
							<span>비밀번호</span>
								{
								showForgetPassword && 
								<span 
									onClick={() => onForgetPasswordClick} 
									className="cursor-pointer font-size-sm font-weight-normal text-muted"
								>
									비밀번호 찾기
								</span>
							} 
						</div>
					} 
					rules={[
						{ 
							required: true,
							message: '비밀번호를 입력해 주세요.',
						}
					]}
				>
					<Input.Password prefix={<LockOutlined className="text-primary" />}/>
				</Form.Item>
				<Form.Item>
					<Button type="primary" htmlType="submit" block loading={loading}>
						로그인
					</Button>
				</Form.Item>
				{ extra }
			</Form>
		</>
	)
}

LoginForm.propTypes = {
	showForgetPassword: PropTypes.bool,
	extra: PropTypes.oneOfType([
		PropTypes.string,
		PropTypes.element
	]),
};

LoginForm.defaultProps = {
	showForgetPassword: false
};

const mapStateToProps = ({auth}) => {
	const {loading, message, showMessage, token, redirect} = auth;
  return {loading, message, showMessage, token, redirect}
}

const mapDispatchToProps = {
	signIn,
	showAuthMessage,
	showLoading,
	hideAuthMessage,
}

export default connect(mapStateToProps, mapDispatchToProps)(LoginForm)
