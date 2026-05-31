import {
  DashboardOutlined,
  AppstoreOutlined,
  DotChartOutlined,
  CalendarOutlined,
  ShoppingCartOutlined,
  ShopOutlined,
  DollarOutlined,
  BarChartOutlined,
  CarOutlined,
  UndoOutlined,
  CommentOutlined,
  CheckCircleOutlined,
  FileDoneOutlined,
  ShoppingOutlined,
  UserOutlined,
  StarOutlined,
  QuestionCircleOutlined,
  MessageOutlined,
  SafetyOutlined
} from '@ant-design/icons';
import { APP_PREFIX_PATH } from 'configs/AppConfig'

const dashBoardNavTree = [{
  key: 'dashboards',
  path: `${APP_PREFIX_PATH}/dashboards`,
  title: '대시보드',
  icon: DashboardOutlined,
  breadcrumb: false,
  isGroupTitle: true,
  submenu: [
    {
      key: 'dashboards-default',
      path: `${APP_PREFIX_PATH}/dashboards/default`,
      title: '기본 대시보드',
      icon: DashboardOutlined,
      breadcrumb: false,
      submenu: []
    },
    {
      key: 'dashboards-analytic',
      path: `${APP_PREFIX_PATH}/dashboards/analytic`,
      title: '운영 상세',
      icon: DotChartOutlined,
      breadcrumb: false,
      submenu: []
    }
  ]
}]

const appsNavTree = [{
  key: 'apps',
  path: `${APP_PREFIX_PATH}/apps`,
  title: '업무 관리',
  icon: AppstoreOutlined,
  breadcrumb: false,
  isGroupTitle: true,
  submenu: [
    {
      key: 'apps-admin-promotion',
      path: '',
      title: '프로모션 운영',
      icon: CalendarOutlined,
      breadcrumb: false,
      roles: ['ADMIN'],
      submenu: [
        {
          key: 'apps-admin-promotion-event-operation',
          path: '',
          title: '이벤트 운영',
          icon: CalendarOutlined,
          breadcrumb: false,
          submenu: [
            {
              key: 'apps-admin-promotion-event-operation-list',
              path: `${APP_PREFIX_PATH}/apps/admin/event-management`,
              title: '이벤트 운영 목록',
              icon: '',
              breadcrumb: true,
              submenu: []
            },
            {
              key: 'apps-admin-promotion-event-operation-performance',
              path: `${APP_PREFIX_PATH}/apps/admin/event-management?tab=performance`,
              title: '이벤트 실적',
              icon: '',
              breadcrumb: true,
              submenu: []
            }
          ]
        },
        {
          key: 'apps-admin-promotion-sales-approval',
          path: `${APP_PREFIX_PATH}/apps/admin/sales-approval`,
          title: '세일 승인',
          icon: DollarOutlined,
          breadcrumb: true,
          submenu: []
        }
      ]
    },
    {
      key: 'apps-admin-partner-operations',
      path: '',
      title: '파트너 운영',
      icon: ShopOutlined,
      breadcrumb: false,
      roles: ['ADMIN'],
      submenu: [
        {
          key: 'apps-admin-partner-operations-approval',
          path: `${APP_PREFIX_PATH}/apps/admin/partner-approval`,
          title: '파트너 입점 승인',
          icon: CheckCircleOutlined,
          breadcrumb: true,
          submenu: []
        },
        {
          key: 'apps-admin-partner-operations-management',
          path: `${APP_PREFIX_PATH}/apps/admin/partner-management`,
          title: '파트너 관리',
          icon: ShopOutlined,
          breadcrumb: true,
          submenu: []
        },
        {
          key: 'apps-admin-partner-operations-change-requests',
          path: `${APP_PREFIX_PATH}/apps/admin/partner-change-requests`,
          title: '파트너 정보 변경 승인',
          icon: FileDoneOutlined,
          breadcrumb: true,
          submenu: []
        }
      ]
    },
    {
      key: 'apps-admin-commerce-operations',
      path: '',
      title: '상품/주문 운영',
      icon: ShoppingCartOutlined,
      breadcrumb: false,
      roles: ['ADMIN'],
      submenu: [
        {
          key: 'apps-admin-commerce-operations-product-approval',
          path: `${APP_PREFIX_PATH}/apps/admin/product-approval`,
          title: '상품 승인',
          icon: ShoppingOutlined,
          breadcrumb: true,
          submenu: []
        },
        {
          key: 'apps-admin-commerce-operations-inventory',
          path: `${APP_PREFIX_PATH}/apps/admin/inventory`,
          title: '재고 관리',
          icon: ShoppingCartOutlined,
          breadcrumb: true,
          submenu: []
        },
        {
          key: 'apps-admin-commerce-operations-order',
          path: `${APP_PREFIX_PATH}/apps/order`,
          title: '주문 관리',
          icon: ShoppingCartOutlined,
          breadcrumb: true,
          submenu: [],
        },
        {
          key: 'apps-admin-commerce-operations-delivery',
          path: `${APP_PREFIX_PATH}/apps/delivery`,
          title: '배송 관리',
          icon: CarOutlined,
          breadcrumb: true,
          submenu: []
        },
        {
          key: 'apps-admin-commerce-operations-return',
          path: `${APP_PREFIX_PATH}/apps/return`,
          title: '반품 관리',
          icon: UndoOutlined,
          breadcrumb: true,
          submenu: []
        },
        {
          key: 'apps-admin-commerce-operations-review',
          path: `${APP_PREFIX_PATH}/apps/review`,
          title: '리뷰 관리',
          icon: CommentOutlined,
          breadcrumb: true,
          submenu: []
        }
      ]
    },
    {
      key: 'apps-admin-customer-settlement',
      path: '',
      title: '고객/정산',
      icon: UserOutlined,
      breadcrumb: false,
      roles: ['ADMIN'],
      submenu: [
        {
          key: 'apps-admin-customer-settlement-customer-management',
          path: `${APP_PREFIX_PATH}/apps/admin/customer-management`,
          title: '고객 관리',
          icon: UserOutlined,
          breadcrumb: true,
          submenu: []
        },
        {
          key: 'apps-admin-customer-settlement-customer-grade-management',
          path: `${APP_PREFIX_PATH}/apps/admin/customer-grade-management`,
          title: '고객 등급 관리',
          icon: StarOutlined,
          breadcrumb: true,
          submenu: []
        },
        {
          key: 'apps-admin-customer-settlement-settlement',
          path: `${APP_PREFIX_PATH}/apps/admin/settlement`,
          title: '정산 조회',
          icon: DollarOutlined,
          breadcrumb: true,
          submenu: []
        },
        {
          key: 'apps-admin-customer-settlement-sales-statistics',
          path: `${APP_PREFIX_PATH}/apps/admin/sales-statistics`,
          title: '매출 현황',
          icon: BarChartOutlined,
          breadcrumb: true,
          submenu: []
        }
      ]
    },
    {
      key: 'apps-admin-operations-settings',
      path: '',
      title: '운영 설정',
      icon: AppstoreOutlined,
      breadcrumb: false,
      roles: ['ADMIN'],
      submenu: [
        {
          key: 'apps-admin-operations-settings-faq-management',
          path: `${APP_PREFIX_PATH}/apps/admin/faq-management`,
          title: 'FAQ 관리',
          icon: QuestionCircleOutlined,
          breadcrumb: true,
          submenu: []
        },
        {
          key: 'apps-admin-operations-settings-qna-management',
          path: `${APP_PREFIX_PATH}/apps/admin/qna-management`,
          title: 'QnA 관리',
          icon: MessageOutlined,
          breadcrumb: true,
          submenu: []
        },
        {
          key: 'apps-admin-operations-settings-color-management',
          path: `${APP_PREFIX_PATH}/apps/admin/color-management`,
          title: '컬러 관리',
          icon: AppstoreOutlined,
          breadcrumb: true,
          submenu: []
        },
        {
          key: 'apps-admin-operations-settings-ai-ops',
          path: `${APP_PREFIX_PATH}/apps/admin/ai-ops`,
          title: 'AI 운영 모니터링',
          icon: SafetyOutlined,
          breadcrumb: true,
          submenu: []
        }
      ]
    },
    {
      key: 'partner-product-operations',
      path: '',
      title: '상품 운영',
      icon: ShoppingCartOutlined,
      breadcrumb: false,
      submenu: [
        {
          key: 'partner-products',
          path: `${APP_PREFIX_PATH}/partner/products`,
          title: '상품 관리',
          icon: ShoppingCartOutlined,
          breadcrumb: true,
          submenu: []
        },
        {
          key: 'partner-inventory',
          path: `${APP_PREFIX_PATH}/partner/inventory`,
          title: '재고 관리',
          icon: ShopOutlined,
          breadcrumb: true,
          submenu: []
        }
      ],
      roles: ['PARTNER']
    },
    {
      key: 'partner-order-settlement',
      path: '',
      title: '주문/정산',
      icon: DollarOutlined,
      breadcrumb: false,
      submenu: [
        {
          key: 'partner-order',
          path: `${APP_PREFIX_PATH}/partner/order`,
          title: '주문 관리',
          icon: ShoppingCartOutlined,
          breadcrumb: true,
          submenu: []
        },
        {
          key: 'partner-settlement',
          path: `${APP_PREFIX_PATH}/partner/settlement`,
          title: '정산 조회',
          icon: DollarOutlined,
          breadcrumb: true,
          submenu: []
        },
        {
          key: 'partner-sales-statistics',
          path: `${APP_PREFIX_PATH}/partner/sales-statistics`,
          title: '매출 현황',
          icon: BarChartOutlined,
          breadcrumb: true,
          submenu: []
        },
        {
          key: 'partner-qna',
          path: `${APP_PREFIX_PATH}/partner/qna`,
          title: 'QnA',
          icon: MessageOutlined,
          breadcrumb: true,
          submenu: []
        }
      ],
      roles: ['PARTNER']
    },
    {
      key: 'partner-promotion-operations',
      path: '',
      title: '프로모션 운영',
      icon: CalendarOutlined,
      breadcrumb: false,
      submenu: [
        {
          key: 'partner-events',
          path: '',
          title: '이벤트 관리',
          icon: CalendarOutlined,
          breadcrumb: false,
          submenu: [
            {
              key: 'partner-events-list',
              path: `${APP_PREFIX_PATH}/partner/events`,
              title: '이벤트 목록',
              icon: '',
              breadcrumb: true,
              submenu: []
            },
            {
              key: 'partner-events-performance',
              path: `${APP_PREFIX_PATH}/partner/events/performance`,
              title: '이벤트 실적',
              icon: '',
              breadcrumb: true,
              submenu: []
            }
          ]
        },
        {
          key: 'partner-sales',
          path: `${APP_PREFIX_PATH}/partner/sales`,
          title: '세일 관리',
          icon: DollarOutlined,
          breadcrumb: true,
          submenu: []
        }
      ],
      roles: ['PARTNER']
    },
    {
      key: 'partner-settings-group',
      path: '',
      title: '설정',
      icon: ShopOutlined,
      breadcrumb: false,
      submenu: [
        {
          key: 'partner-settings',
          path: `${APP_PREFIX_PATH}/partner/settings`,
          title: '파트너 설정',
          icon: ShopOutlined,
          breadcrumb: true,
          submenu: []
        }
      ],
      roles: ['PARTNER']
    }
  ]
}]

const navigationConfig = [
  ...dashBoardNavTree,
  ...appsNavTree
]

export default navigationConfig;