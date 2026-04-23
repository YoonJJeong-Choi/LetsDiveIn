import React, { useEffect, useMemo, useState } from 'react'
import { Alert, Card, Modal, Select, Space, Table, Tag, Typography, Button, Statistic, Tooltip, Switch, Input } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import SalePolicyAdminService from 'services/SalePolicyAdminService'
import AdminService from 'services/AdminService'

const { Title } = Typography

const STATUS_LABELS = {
  PENDING_APPROVAL: '\uC2B9\uC778 \uB300\uAE30',
  ACTIVE: '\uD65C\uC131',
  INACTIVE: '\uBE44\uD65C\uC131',
  EXPIRED: '\uB9CC\uB8CC',
  CANCELLED: '\uC138\uC77C \uC911\uB2E8',
  REJECTED: '\uC2B9\uC778 \uAC70\uC808'
}

const STATUS_COLORS = {
  PENDING_APPROVAL: 'orange',
  ACTIVE: 'green',
  INACTIVE: 'default',
  EXPIRED: 'default',
  CANCELLED: 'default',
  REJECTED: 'red'
}

const SalesApprovalPage = () => {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [list, setList] = useState([])
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [todayOnly, setTodayOnly] = useState(false)
  const [detailOpen, setDetailOpen] = useState(false)
  const [detailPolicies, setDetailPolicies] = useState([])
  const [detailMeta, setDetailMeta] = useState(null)
  const [products, setProducts] = useState([])

  const pendingCount = useMemo(() => list.filter((s) => s.status === 'PENDING_APPROVAL').length, [list])
  const detailTotalCount = detailPolicies?.length || 0
  const detailPendingCount = (detailPolicies || []).filter((p) => p.status === 'PENDING_APPROVAL').length

  const fetchSales = async () => {
    setError(null)
    setLoading(true)
    try {
      const res = await SalePolicyAdminService.listAdminSales()
      const sales = Array.isArray(res?.data) ? res.data : Array.isArray(res) ? res : []
      setList(sales)
    } catch (e) {
      setError(e?.message || '\uC138\uC77C \uC815\uCC45 \uBAA9\uB85D \uC870\uD68C\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.')
    } finally {
      setLoading(false)
    }
  }

  const fetchProducts = async () => {
    try {
      const res = await AdminService.getAllProducts(null)
      const list = Array.isArray(res)
        ? res
        : Array.isArray(res?.data)
          ? res.data
          : Array.isArray(res?.data?.data)
            ? res.data.data
            : []
      setProducts(list)
    } catch (e) {
      // 상품명 표시용 보조 데이터라 실패해도 화면 동작은 유지합니다.
      setProducts([])
    }
  }

  useEffect(() => {
    fetchSales()
    fetchProducts()
  }, [])

  const productNameByNo = useMemo(() => {
    const map = new Map()
    ;(products || []).forEach((p) => {
      map.set(String(p.productNo), p.productName || '-')
    })
    return map
  }, [products])

  const optionLabelByNo = useMemo(() => {
    const map = new Map()
    ;(products || []).forEach((p) => {
      ;(p.options || []).forEach((opt) => {
        const optionLabel = `${opt.color || '-'}${opt.color && opt.size ? ' / ' : ''}${opt.size || ''}`.trim()
        map.set(String(opt.optionNo), {
          productName: p.productName || '-',
          optionLabel: optionLabel || '-'
        })
      })
    })
    return map
  }, [products])

  const saleStatusRank = (status) => ({
    ACTIVE: 0,
    PENDING_APPROVAL: 1,
    INACTIVE: 2,
    EXPIRED: 3,
    CANCELLED: 4,
    REJECTED: 5
  }[status] ?? 99)

  const groupedList = useMemo(() => {
    const groups = new Map()
    ;(list || []).forEach((p) => {
      const key = p.campaignId ? `campaign:${p.campaignId}` : `single:${p.id}`
      if (!groups.has(key)) groups.set(key, [])
      groups.get(key).push(p)
    })

    return Array.from(groups.entries()).map(([key, policies]) => {
      const first = policies[0]
      const best = policies.reduce((acc, cur) =>
        saleStatusRank(cur.status) < saleStatusRank(acc.status) ? cur : acc
      , policies[0])
      return {
        id: key,
        campaignId: first.campaignId || null,
        policies,
        scope: first.scope,
        targetCount: policies.length,
        startAt: first.startAt,
        endAt: first.endAt,
        status: best.status
      }
    })
  }, [list])

  const visibleList = useMemo(() => {
    let filtered = groupedList
    if (statusFilter && statusFilter !== 'ALL') filtered = filtered.filter((s) => s.status === statusFilter)

    if (todayOnly) {
      const now = dayjs()
      filtered = filtered.filter((s) => {
        const inPeriod = !now.isBefore(dayjs(s.startAt)) && !now.isAfter(dayjs(s.endAt))
        return s.status === 'ACTIVE' && inPeriod
      })
    }
    return filtered
  }, [groupedList, statusFilter, todayOnly])

  const handleApprove = async (record) => {
    Modal.confirm({
      title: '\uC138\uC77C \uC2B9\uC778',
      content: `\uB300\uC0C1: ${record.scope === 'OPTION' ? `\uC635\uC158 ${record.targetCount}\uAC1C` : `\uC0C1\uD488 ${record.targetCount}\uAC1C`}`,
      okText: '\uC2B9\uC778',
      okType: 'primary',
      cancelText: '\uCDE8\uC18C',
      onOk: async () => {
        try {
          for (const p of record.policies || []) {
            if (p.status === 'PENDING_APPROVAL') {
              // eslint-disable-next-line no-await-in-loop
              await SalePolicyAdminService.approveSale(p.id)
            }
          }
          await fetchSales()
        } catch (e) {
          Modal.error({ title: '\uC2B9\uC778 \uC2E4\uD328', content: e?.message || '\uC138\uC77C \uC2B9\uC778\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.' })
        }
      }
    })
  }

  const handleReject = async (record) => {
    let rejectionReason = ''
    Modal.confirm({
      title: '\uC2B9\uC778 \uAC70\uC808',
      content: (
        <Space direction="vertical" style={{ width: '100%' }}>
          <span>해당 승인 대기 세일을 거절하시겠습니까?</span>
          <Input.TextArea
            rows={3}
            maxLength={500}
            placeholder="거절 사유를 입력해주세요. (필수)"
            onChange={(e) => { rejectionReason = e.target.value }}
          />
        </Space>
      ),
      okText: '\uC2B9\uC778 \uAC70\uC808',
      okType: 'danger',
      cancelText: '\uB2EB\uAE30',
      onOk: async () => {
        const reason = (rejectionReason || '').trim()
        if (!reason) {
          Modal.error({ title: '입력 필요', content: '거절 사유를 입력해주세요.' })
          return Promise.reject(new Error('rejectionReason required'))
        }
        try {
          for (const p of record.policies || []) {
            if (p.status === 'PENDING_APPROVAL') {
              // eslint-disable-next-line no-await-in-loop
              await SalePolicyAdminService.rejectSale(p.id, reason)
            }
          }
          await fetchSales()
        } catch (e) {
          Modal.error({ title: '\uC2B9\uC778 \uAC70\uC808 \uC2E4\uD328', content: e?.message || '\uC138\uC77C \uC2B9\uC778 \uAC70\uC808\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.' })
        }
      }
    })
  }

  const handleCancel = async (record) => {
    let reason = ''
    Modal.confirm({
      title: '\uC138\uC77C \uC911\uB2E8',
      content: (
        <Space direction="vertical" style={{ width: '100%' }}>
          <div>\uD574\uB2F9 \uC138\uC77C\uC744 \uC911\uB2E8\uD558\uC2DC\uACA0\uC2B5\uB2C8\uAE4C? (\uC911\uB2E8 \uD6C4 \uD560\uC778\uC740 \uC801\uC6A9\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4.)</div>
          <Input.TextArea
            rows={3}
            placeholder="\uC911\uB2E8 \uC0AC\uC720\uB97C \uC785\uB825\uD574\uC8FC\uC138\uC694."
            onChange={(e) => { reason = e.target.value }}
          />
        </Space>
      ),
      okText: '\uD655\uC778',
      cancelText: '\uB2EB\uAE30',
      onOk: async () => {
        try {
          if (!reason || !reason.trim()) {
            Modal.error({ title: '입력 필요', content: '중단 사유를 입력해주세요.' })
            return Promise.reject(new Error('cancel reason required'))
          }
          for (const p of record.policies || []) {
            if (p.status !== 'CANCELLED' && p.status !== 'EXPIRED') {
              // eslint-disable-next-line no-await-in-loop
              await SalePolicyAdminService.cancelSale(p.id, reason.trim())
            }
          }
          await fetchSales()
        } catch (e) {
          Modal.error({ title: '\uC138\uC77C \uC911\uB2E8 \uC2E4\uD328', content: e?.message || '\uC138\uC77C \uC911\uB2E8\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.' })
        }
      }
    })
  }

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 140,
      render: (_, record) => {
        if (!record.campaignId) return `\uC815\uCC45:${record.policies[0]?.id}`
        const shortId = String(record.campaignId).slice(0, 8)
        return (
          <Tooltip title={record.campaignId}>
            <span>{`\uCEA0\uD398\uC778 #${shortId}`}</span>
          </Tooltip>
        )
      }
    },
    {
      title: '\uBC94\uC704',
      dataIndex: 'scope',
      width: 120,
      render: (v) => (v === 'OPTION' ? '\uC635\uC158' : '\uC0C1\uD488')
    },
    {
      title: '\uB300\uC0C1',
      key: 'target',
      render: (_, record) =>
        record.scope === 'OPTION'
          ? `\uC635\uC158 ${record.targetCount}\uAC1C`
          : `\uC0C1\uD488 ${record.targetCount}\uAC1C`
    },
    {
      title: '신청 파트너',
      key: 'partner',
      width: 180,
      render: (_, record) => {
        const p = record.policies?.[0]
        if (!p?.createdByPartnerId) return '-'
        return `${p.createdByPartnerName || '-'} (#${p.createdByPartnerId})`
      }
    },
    {
      title: '\uAE30\uAC04',
      key: 'period',
      render: (_, record) => {
        const startAt = record.startAt ? dayjs(record.startAt).format('YYYY-MM-DD') : '-'
        const endAt = record.endAt ? dayjs(record.endAt).format('YYYY-MM-DD') : '-'
        return `${startAt} ~ ${endAt}`
      }
    },
    {
      title: '\uC0C1\uD0DC',
      width: 140,
      render: (_, record) => {
        const pendingCountInGroup = (record.policies || []).filter((p) => p.status === 'PENDING_APPROVAL').length
        return (
          <Space size={6}>
            <Tag color={STATUS_COLORS[record.status] || 'default'}>
              {STATUS_LABELS[record.status] || record.status}
            </Tag>
            {pendingCountInGroup > 0 && record.status !== 'PENDING_APPROVAL' && (
              <Tag color="orange">{`\uC2B9\uC778 \uB300\uAE30 ${pendingCountInGroup}\uAC74`}</Tag>
            )}
          </Space>
        )
      }
    },
    {
      title: '\uC791\uC5C5',
      key: 'action',
      width: 100,
      render: (_, record) => (
        <Space>
          <Button
            size="small"
            onClick={() => {
              setDetailPolicies(record.policies)
              setDetailMeta(record)
              setDetailOpen(true)
            }}
          >
            {'\uC0C1\uC138'}
          </Button>
        </Space>
      )
    }
  ]

  return (
    <Card>
      <Space style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', width: '100%' }}>
        <Title level={4} style={{ margin: 0 }}>{'\uC138\uC77C \uC2B9\uC778'}</Title>
        <Statistic title={'\uC2B9\uC778 \uB300\uAE30'} value={pendingCount} />
      </Space>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message={'\uD560\uC778\uC728/\uAE30\uAC04 \uAE30\uC900\uC744 \uCD08\uACFC\uD55C \uAC74\uC740, \uAD00\uB9AC\uC790 \uC2B9\uC778 \uD6C4\uC5D0\uB9CC \uC801\uC6A9\uB429\uB2C8\uB2E4. 40% \uCD08\uACFC, 14\uC77C \uCD08\uACFC'}
      />

      <Space style={{ marginBottom: 16 }} wrap>
        <Select
          value={statusFilter}
          style={{ width: 220 }}
          onChange={setStatusFilter}
          options={[
            { value: 'ALL', label: '전체' },
            { value: 'PENDING_APPROVAL', label: '\uC2B9\uC778 \uB300\uAE30' },
            { value: 'ACTIVE', label: '\uD65C\uC131' },
            { value: 'REJECTED', label: '\uC2B9\uC778 \uAC70\uC808' },
            { value: 'CANCELLED', label: '\uC138\uC77C \uC911\uB2E8' },
            { value: 'EXPIRED', label: '\uB9CC\uB8CC' }
          ]}
        />
        <Space size={6}>
          <Switch checked={todayOnly} onChange={setTodayOnly} />
          <span>{'\uC624\uB298 \uC138\uC77C \uC911'}</span>
        </Space>
        <Button icon={<ReloadOutlined />} onClick={fetchSales} loading={loading}>
          {'\uC0C8\uB85C\uACE0\uCE68'}
        </Button>
      </Space>

      {error && (
        <Alert type="error" showIcon message={'\uC624\uB958'} description={error} style={{ marginBottom: 16 }} />
      )}

      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={visibleList}
        pagination={{ pageSize: 10, showSizeChanger: true }}
        locale={{ emptyText: '\uD45C\uC2DC\uD560 \uC138\uC77C \uC815\uCC45\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.' }}
      />

      <Modal
        title={null}
        open={detailOpen}
        width={980}
        onCancel={() => {
          setDetailOpen(false)
          setDetailMeta(null)
        }}
        footer={[
          detailMeta?.status === 'PENDING_APPROVAL' && (
            <Button key="approve" type="primary" onClick={() => detailMeta && handleApprove(detailMeta)}>
              {'\uC2B9\uC778'}
            </Button>
          ),
          detailMeta?.status === 'PENDING_APPROVAL' && (
            <Button key="reject" danger onClick={() => detailMeta && handleReject(detailMeta)}>
              {'\uC2B9\uC778 \uAC70\uC808'}
            </Button>
          ),
          detailMeta?.status === 'ACTIVE' && (
            <Button
              key="cancel"
              danger
              onClick={() => detailMeta && handleCancel(detailMeta)}
            >
              {'\uC138\uC77C \uC911\uB2E8'}
            </Button>
          ),
          <Button key="close" onClick={() => {
            setDetailOpen(false)
            setDetailMeta(null)
          }}>
            {'\uB2EB\uAE30'}
          </Button>
        ]}
      >
        <Space style={{ marginBottom: 12 }}>
          <Tag>{`\uCD1D ${detailTotalCount}\uAC74`}</Tag>
          {detailPendingCount > 0 && <Tag color="orange">{`\uC2B9\uC778 \uB300\uAE30 ${detailPendingCount}\uAC74`}</Tag>}
        </Space>
        {detailPolicies && detailPolicies.length > 0 ? (
          <Table
            size="small"
            rowKey="id"
            pagination={false}
            scroll={{ x: 900 }}
            dataSource={detailPolicies}
            columns={[
              { title: 'ID', dataIndex: 'id', width: 80 },
              {
                title: '\uBC94\uC704',
                width: 90,
                render: (_, r) => (r.scope === 'OPTION' ? '\uC635\uC158' : '\uC0C1\uD488')
              },
              {
                title: '\uB300\uC0C1',
                width: 240,
                render: (_, r) => {
                  if (r.scope === 'OPTION') {
                    const optionInfo = optionLabelByNo.get(String(r.targetOptionNo))
                    if (optionInfo) {
                      return `${optionInfo.productName} (${optionInfo.optionLabel})`
                    }
                    return `\uC635\uC158 #${r.targetOptionNo}`
                  }
                  return productNameByNo.get(String(r.targetProductNo)) || `\uC0C1\uD488 #${r.targetProductNo}`
                }
              },
              {
                title: '신청 파트너',
                width: 180,
                render: (_, r) =>
                  r.createdByPartnerId
                    ? `${r.createdByPartnerName || '-'} (#${r.createdByPartnerId})`
                    : '-'
              },
              {
                title: '\uD560\uC778',
                render: (_, r) =>
                  r.discountType === 'PERCENT'
                    ? `${r.discountValue}%`
                    : `${Number(r.discountValue || 0).toLocaleString()}\uC6D0`
              },
              {
                title: '\uC0C1\uD0DC',
                width: 140,
                render: (_, r) => (
                  <Tag color={STATUS_COLORS[r.status] || 'default'}>{STATUS_LABELS[r.status] || r.status}</Tag>
                )
              },
              {
                title: '거절 사유',
                width: 220,
                render: (_, r) => r.rejectionReason || '-'
              },
              {
                title: '\uAE30\uAC04',
                width: 170,
                render: (_, r) => `${dayjs(r.startAt).format('YYYY-MM-DD')} ~ ${dayjs(r.endAt).format('YYYY-MM-DD')}`
              }
            ]}
          />
        ) : (
          <p>{'\uD45C\uC2DC\uD560 \uD56D\uBAA9\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.'}</p>
        )}
      </Modal>
    </Card>
  )
}

export default SalesApprovalPage