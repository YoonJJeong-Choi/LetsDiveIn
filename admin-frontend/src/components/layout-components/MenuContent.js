import React, { useMemo, useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Menu, Grid } from 'antd';
import IntlMessage from '../util-components/IntlMessage';
import Icon from '../util-components/Icon';
import navigationConfig from 'configs/NavigationConfig';
import { useSelector, useDispatch } from 'react-redux';
import { SIDE_NAV_LIGHT, NAV_TYPE_SIDE } from "constants/ThemeConstant";
import utils from 'utils'
import { onMobileNavToggle } from 'store/slices/themeSlice';

const { useBreakpoint } = Grid;

const setLocale = (localeKey, isLocaleOn = true) =>
	isLocaleOn ? <IntlMessage id={localeKey} /> : localeKey.toString();

/** 현재 메뉴의 부모 SubMenu key 목록 (NavigationConfig 트리 기준) */
const findMenuAncestorKeys = (navItems, targetKey, ancestors = []) => {
	if (!targetKey) {
		return [];
	}
	for (const nav of navItems) {
		if (nav.key === targetKey) {
			return ancestors;
		}
		if (nav.submenu?.length > 0) {
			const found = findMenuAncestorKeys(nav.submenu, targetKey, [...ancestors, nav.key]);
			if (found) {
				return found;
			}
		}
	}
	return null;
};

const resolveOpenKeys = (routeKey) => findMenuAncestorKeys(navigationConfig, routeKey) ?? [];

const MenuItem = ({title, icon, path}) => {

	const dispatch = useDispatch();

	const isMobile = !utils.getBreakPoint(useBreakpoint()).includes('lg');

	const closeMobileNav = () => {
		if (isMobile) {
			dispatch(onMobileNavToggle(false))
		}
	}

	return (
		<>
			{icon && <Icon type={icon} /> }
			<span>{setLocale(title)}</span>
			{path && <Link onClick={closeMobileNav} to={path} />}
		</>
	)
}

const getSideNavMenuItem = (navItem, userRole) => navItem
	.filter(nav => {
		// role 체크: roles가 정의되어 있으면 해당 role만 허용
		if (nav.roles && nav.roles.length > 0) {
			return nav.roles.includes(userRole);
		}
		// role 체크가 없으면 모두 허용
		return true;
	})
	.map(nav => {
		return {
			key: nav.key,
			label: <MenuItem title={nav.title} {...(nav.isGroupTitle ? {} : {path: nav.path, icon: nav.icon})} />,
			...(nav.isGroupTitle ? {type: 'group'} : {}),
			...(nav.submenu.length > 0 ? {children: getSideNavMenuItem(nav.submenu, userRole)} : {})
		}
	})

const getTopNavMenuItem = (navItem, userRole) => navItem
	.filter(nav => {
		// role 체크: roles가 정의되어 있으면 해당 role만 허용
		if (nav.roles && nav.roles.length > 0) {
			return nav.roles.includes(userRole);
		}
		// role 체크가 없으면 모두 허용
		return true;
	})
	.map(nav => {
		return {
			key: nav.key,
			label: <MenuItem title={nav.title} icon={nav.icon} {...(nav.isGroupTitle ? {} : {path: nav.path})} />,
			...(nav.submenu.length > 0 ? {children: getTopNavMenuItem(nav.submenu, userRole)} : {})
		}
	})

const SideNavContent = (props) => {

	const { routeInfo, hideGroupTitle, sideNavTheme = SIDE_NAV_LIGHT } = props;
	const { user } = useSelector(state => state.auth);
	const userRole = user?.role || null;
	const routeKey = routeInfo?.key;

	const menuItems = useMemo(() => getSideNavMenuItem(navigationConfig, userRole), [userRole]);

	const [openKeys, setOpenKeys] = useState(() => resolveOpenKeys(routeKey));

	useEffect(() => {
		setOpenKeys(resolveOpenKeys(routeKey));
	}, [routeKey]);

	return (
		<Menu
			mode="inline"
			theme={sideNavTheme === SIDE_NAV_LIGHT ? "light" : "dark"}
			style={{ height: "100%", borderInlineEnd: 0 }}
			selectedKeys={routeKey ? [routeKey] : []}
			openKeys={openKeys}
			onOpenChange={setOpenKeys}
			className={hideGroupTitle ? "hide-group-title" : ""}
			items={menuItems}
		/>
	);
};

const TopNavContent = () => {

	const topNavColor = useSelector(state => state.theme.topNavColor);
	const { user } = useSelector(state => state.auth);
	const userRole = user?.role || null;

	const menuItems = useMemo(() => getTopNavMenuItem(navigationConfig, userRole), [userRole])

	return (
		<Menu 
			mode="horizontal" 
			style={{ backgroundColor: topNavColor }}
			items={menuItems}
		/>
	);
};

const MenuContent = (props) => {
	return props.type === NAV_TYPE_SIDE ? (
		<SideNavContent {...props} />
	) : (
		<TopNavContent {...props} />
	);
};

export default MenuContent;
