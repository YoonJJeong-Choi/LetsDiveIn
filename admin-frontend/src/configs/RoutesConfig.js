import React from 'react'
import { AUTH_PREFIX_PATH, APP_PREFIX_PATH } from 'configs/AppConfig'

export const publicRoutes = [
    {
        key: 'login',
        path: `${AUTH_PREFIX_PATH}/login`,
        component: React.lazy(() => import('views/auth-views/authentication/login')),
    },
    {
        key: 'forgot-password',
        path: `${AUTH_PREFIX_PATH}/forgot-password`,
        component: React.lazy(() => import('views/auth-views/authentication/forgot-password')),
    },
    {
        key: 'error-page-1',
        path: `${AUTH_PREFIX_PATH}/error-page-1`,
        component: React.lazy(() => import('views/auth-views/errors/error-page-1')),
    },
    {
        key: 'error-page-2',
        path: `${AUTH_PREFIX_PATH}/error-page-2`,
        component: React.lazy(() => import('views/auth-views/errors/error-page-2')),
    },
]

const protectedRouteDefinitions = [
    {
        key: 'dashboard.default',
        path: `${APP_PREFIX_PATH}/dashboards/default`,
        component: React.lazy(() => import('views/app-views/dashboards/default')),
    },
    {
        key: 'dashboard.analytic',
        path: `${APP_PREFIX_PATH}/dashboards/analytic`,
        component: React.lazy(() => import('views/app-views/dashboards/analytic')),
    },
    {
        key: 'partner.products',
        path: `${APP_PREFIX_PATH}/partner/products`,
        component: React.lazy(() => import('views/app-views/apps/partner/products')),
    },
    {
        key: 'partner.products.create',
        path: `${APP_PREFIX_PATH}/partner/products/create`,
        component: React.lazy(() => import('views/app-views/apps/partner/products/ProductCreatePage')),
    },
    {
        key: 'partner.products.edit',
        path: `${APP_PREFIX_PATH}/partner/products/edit/:productNo`,
        component: React.lazy(() => import('views/app-views/apps/partner/products/ProductEditPage')),
    },
    {
        key: 'partner.inventory',
        path: `${APP_PREFIX_PATH}/partner/inventory`,
        component: React.lazy(() => import('views/app-views/apps/partner/inventory')),
    },
    {
        key: 'partner.settlement',
        path: `${APP_PREFIX_PATH}/partner/settlement`,
        component: React.lazy(() => import('views/app-views/apps/partner/settlement')),
    },
    {
        key: 'partner.sales-statistics',
        path: `${APP_PREFIX_PATH}/partner/sales-statistics`,
        component: React.lazy(() => import('views/app-views/apps/partner/sales-statistics')),
    },
    {
        key: 'partner.settings',
        path: `${APP_PREFIX_PATH}/partner/settings`,
        component: React.lazy(() => import('views/app-views/apps/partner/settings')),
    },
    {
        key: 'partner.order',
        path: `${APP_PREFIX_PATH}/partner/order`,
        component: React.lazy(() => import('views/app-views/apps/partner/order')),
    },
    {
        key: 'partner.events',
        path: `${APP_PREFIX_PATH}/partner/events`,
        component: React.lazy(() => import('views/app-views/apps/partner/events')),
    },
    {
        key: 'partner.events.performance',
        path: `${APP_PREFIX_PATH}/partner/events/performance`,
        component: React.lazy(() => import('views/app-views/apps/partner/event-performance')),
    },
    {
        key: 'partner.sales',
        path: `${APP_PREFIX_PATH}/partner/sales`,
        component: React.lazy(() => import('views/app-views/apps/partner/sales')),
    },
    {
        key: 'apps.delivery',
        path: `${APP_PREFIX_PATH}/apps/delivery`,
        component: React.lazy(() => import('views/app-views/apps/delivery')),
    },
    {
        key: 'apps.order',
        path: `${APP_PREFIX_PATH}/apps/order`,
        component: React.lazy(() => import('views/app-views/apps/order')),
    },
    {
        key: 'apps.return',
        path: `${APP_PREFIX_PATH}/apps/return`,
        component: React.lazy(() => import('views/app-views/apps/return')),
    },
    {
        key: 'apps.review',
        path: `${APP_PREFIX_PATH}/apps/review`,
        component: React.lazy(() => import('views/app-views/apps/review')),
    },
    {
        key: 'apps.admin.partner-approval',
        path: `${APP_PREFIX_PATH}/apps/admin/partner-approval`,
        component: React.lazy(() => import('views/app-views/apps/admin/partner-approval')),
    },
    {
        key: 'apps.admin.partner-management',
        path: `${APP_PREFIX_PATH}/apps/admin/partner-management`,
        component: React.lazy(() => import('views/app-views/apps/admin/partner-management')),
    },
    {
      key: 'apps.admin.partner-change-requests',
      path: `${APP_PREFIX_PATH}/apps/admin/partner-change-requests`,
      component: React.lazy(() => import('views/app-views/apps/admin/partner-change-requests')),
    },
    {
        key: 'apps.admin.product-approval',
        path: `${APP_PREFIX_PATH}/apps/admin/product-approval`,
        component: React.lazy(() => import('views/app-views/apps/admin/product-approval')),
    },
    {
        key: 'apps.admin.inventory',
        path: `${APP_PREFIX_PATH}/apps/admin/inventory`,
        component: React.lazy(() => import('views/app-views/apps/admin/inventory')),
    },
    {
        key: 'apps.admin.settlement',
        path: `${APP_PREFIX_PATH}/apps/admin/settlement`,
        component: React.lazy(() => import('views/app-views/apps/admin/settlement')),
    },
    {
        key: 'apps.admin.customer-management',
        path: `${APP_PREFIX_PATH}/apps/admin/customer-management`,
        component: React.lazy(() => import('views/app-views/apps/admin/customer-management')),
    },
    {
        key: 'apps.admin.sales-statistics',
        path: `${APP_PREFIX_PATH}/apps/admin/sales-statistics`,
        component: React.lazy(() => import('views/app-views/apps/admin/sales-statistics')),
    },
    {
        key: 'apps.admin.sales-approval',
        path: `${APP_PREFIX_PATH}/apps/admin/sales-approval`,
        component: React.lazy(() => import('views/app-views/apps/admin/sales-approval')),
    },
    {
        key: 'apps.admin.customer-grade-management',
        path: `${APP_PREFIX_PATH}/apps/admin/customer-grade-management`,
        component: React.lazy(() => import('views/app-views/apps/admin/customer-grade-management')),
    },
    {
        key: 'apps.admin.faq-management',
        path: `${APP_PREFIX_PATH}/apps/admin/faq-management`,
        component: React.lazy(() => import('views/app-views/apps/admin/faq-management')),
    },
    {
        key: 'apps.admin.qna-management',
        path: `${APP_PREFIX_PATH}/apps/admin/qna-management`,
        component: React.lazy(() => import('views/app-views/apps/admin/qna-management')),
    },
    {
        key: 'partner.qna',
        path: `${APP_PREFIX_PATH}/partner/qna`,
        component: React.lazy(() => import('views/app-views/apps/partner/qna')),
    },
    {
        key: 'apps.admin.event-management',
        path: `${APP_PREFIX_PATH}/apps/admin/event-management`,
        component: React.lazy(() => import('views/app-views/apps/admin/event-management')),
    },
    {
        key: 'apps.admin.color-management',
        path: `${APP_PREFIX_PATH}/apps/admin/color-management`,
        component: React.lazy(() => import('views/app-views/apps/admin/color-management')),
    },
    {
        key: 'apps.admin.ai-ops',
        path: `${APP_PREFIX_PATH}/apps/admin/ai-ops`,
        component: React.lazy(() => import('views/app-views/apps/admin/ai-ops')),
    },
    {
        key: 'forgot-password',
        path: `${APP_PREFIX_PATH}/forgot-password`,
        component: React.lazy(() => import('views/auth-views/authentication/forgot-password')),
        meta: {
            blankLayout: true
        }
    },
    {
        key: 'error-page-1',
        path: `${APP_PREFIX_PATH}/error-page-1`,
        component: React.lazy(() => import('views/auth-views/errors/error-page-1')),
        meta: {
            blankLayout: true
        }
    },
    {
        key: 'error-page-2',
        path: `${APP_PREFIX_PATH}/error-page-2`,
        component: React.lazy(() => import('views/auth-views/errors/error-page-2')),
        meta: {
            blankLayout: true
        }
    },
    {
        key: 'pages.faq',
        path: `${APP_PREFIX_PATH}/pages/faq`,
        component: React.lazy(() => import('views/app-views/pages/faq')),
    }
]

export const protectedRoutes = protectedRouteDefinitions
