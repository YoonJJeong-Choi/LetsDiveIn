import React, { useEffect, useState } from 'react';
import { Dropdown, Avatar } from 'antd';
import { useDispatch, useSelector } from 'react-redux'
import { useNavigate } from 'react-router-dom'
import { 
	EditOutlined, 
	SettingOutlined, 
	ShopOutlined, 
	QuestionCircleOutlined, 
	LogoutOutlined 
} from '@ant-design/icons';
import NavItem from './NavItem';
import Flex from 'components/shared-components/Flex';
import { signOut } from 'store/slices/authSlice';
import { APP_PREFIX_PATH, AUTH_PREFIX_PATH, UNAUTHENTICATED_ENTRY } from 'configs/AppConfig';
import styled from '@emotion/styled';
import { FONT_WEIGHT, MEDIA_QUERIES, SPACER, FONT_SIZES } from 'constants/ThemeConstant'
import utils from 'utils'
import PartnerService from 'services/PartnerService'

const Icon = styled.div(() => ({
	fontSize: FONT_SIZES.LG
}))

const Profile = styled.div(() => ({
	display: 'flex',
	alignItems: 'center'
}))

const UserInfo = styled('div')`
	padding-left: ${SPACER[2]};

	@media ${MEDIA_QUERIES.MOBILE} {
		display: none
	}
`

const Name = styled.div(() => ({
	fontWeight: FONT_WEIGHT.SEMIBOLD
}))

const Title = styled.span(() => ({
	opacity: 0.8
}))

const MenuItem = (props) => (
	<Flex alignItems="center" gap={SPACER[2]}>
		<Icon>{props.icon}</Icon>
		<span>{props.label}</span>
	</Flex>
)

const MenuItemSignOut = (props) => {

	const dispatch = useDispatch();
	const navigate = useNavigate();

	const handleSignOut = async () => {
		await dispatch(signOut())
		navigate(`${AUTH_PREFIX_PATH}${UNAUTHENTICATED_ENTRY}`)
	}

	return (
		<div onClick={handleSignOut}>
			<Flex alignItems="center" gap={SPACER[2]} >
				<Icon>
					<LogoutOutlined />
				</Icon>
				<span>{props.label}</span>
			</Flex>
		</div>
	)
}

export const NavProfile = ({mode}) => {
	const navigate = useNavigate();
	const user = useSelector(state => state.auth.user)
	const userName = user?.name || 'Admin'
	const userEmail = user?.email || ''
	const userRole = user?.role || 'ADMIN'
	const isPartner = userRole === 'PARTNER'
	const [avatarUrl, setAvatarUrl] = useState(user?.avatar || null)
	const profilePath = isPartner ? `${APP_PREFIX_PATH}/partner/settings?tab=profile` : `${APP_PREFIX_PATH}/pages/profile`
	const accountSettingPath = isPartner ? `${APP_PREFIX_PATH}/partner/settings` : `${APP_PREFIX_PATH}/pages/setting`
	const accountBillingPath = isPartner ? `${APP_PREFIX_PATH}/partner/settlement` : `${APP_PREFIX_PATH}/apps/admin/settlement`
	const helpCenterPath = `${APP_PREFIX_PATH}/pages/faq`
	
	// 파트너인 경우 프로필 이미지 로드
	useEffect(() => {
		const load = async () => {
			try {
				if (isPartner) {
					const res = await PartnerService.getMyProfile()
					const data = res?.data || res
					if (data?.profileImageUrl) {
						setAvatarUrl(data.profileImageUrl)
					}
				}
			} catch (e) {
				// 무시: 아바타는 필수 아님
			}
		}
		load()
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [isPartner])

	// Role을 한글로 표시
	const roleMap = {
		'ADMIN': '관리자',
		'PARTNER': '파트너',
		'CUSTOMER': '고객'
	}
	const roleTitle = roleMap[userRole] || '사용자'
	const items = [
		{
			key: 'Edit Profile',
			label: <MenuItem label="Edit Profile" icon={<EditOutlined />} />,
			onClick: () => navigate(profilePath),
		},
		{
			key: 'Account Setting',
			label: <MenuItem label="Account Setting" icon={<SettingOutlined />} />,
			onClick: () => navigate(accountSettingPath),
		},
		{
			key: 'Account Billing',
			label: <MenuItem label="Account Billing" icon={<ShopOutlined />} />,
			onClick: () => navigate(accountBillingPath),
		},
		{
			key: 'Help Center',
			label: <MenuItem label="Help Center" icon={<QuestionCircleOutlined />} />,
			onClick: () => navigate(helpCenterPath),
		},
		{
			key: 'Sign Out',
			label: <MenuItemSignOut label="Sign Out" />,
		}
	]

	return (
		<Dropdown placement="bottomRight" menu={{items}} trigger={["click"]}>
			<NavItem mode={mode}>
				<Profile>
					{avatarUrl ? (
						<Avatar src={avatarUrl} />
					) : (
						<Avatar style={{ backgroundColor: '#1890ff' }}>
							{utils.getNameInitial(userName)}
						</Avatar>
					)}
					<UserInfo className="profile-text">
						<Name>{userName}</Name>
						<Title>{roleTitle}</Title>
					</UserInfo>
				</Profile>
			</NavItem>
		</Dropdown>
	);
}

export default NavProfile
