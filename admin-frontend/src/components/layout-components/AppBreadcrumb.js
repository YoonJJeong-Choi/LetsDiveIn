import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Breadcrumb } from 'antd';
import navigationConfig from "configs/NavigationConfig";
import IntlMessage from 'components/util-components/IntlMessage';

let breadcrumbData = {};

const assignBreadcrumbMap = (navItems) => {
	navItems.forEach((item) => {
		if (item?.path) {
			breadcrumbData[item.path] = <IntlMessage id={item.title} />;
		}
		if (item?.submenu?.length) {
			assignBreadcrumbMap(item.submenu);
		}
	});
};

assignBreadcrumbMap(navigationConfig);

const BreadcrumbRoute = props => {
	const location = useLocation();
	const fullPath = `${location.pathname}${location.search}`;
	const pathSnippets = location.pathname.split('/').filter(i => i);
	const breadcrumbItems = pathSnippets.map((_, index) => {
		const url = `/${pathSnippets.slice(0, index + 1).join('/')}`;
		const title = breadcrumbData[url];
		if (!title) {
			return null;
		}
		return {
			title: <Link to={url}>{title}</Link>
		}
	}).filter(Boolean);

	const exactTitle = breadcrumbData[fullPath];
	if (location.search && exactTitle && breadcrumbItems.length > 0) {
		breadcrumbItems[breadcrumbItems.length - 1] = {
			title: exactTitle
		};
	}
  
	return (
		<Breadcrumb items={breadcrumbItems} />
	);
};

export const AppBreadcrumb = () => {
	return <BreadcrumbRoute />
}

export default AppBreadcrumb
