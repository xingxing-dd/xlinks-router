import { request } from '@/utils/http'

export const loginAdmin = (body) => request('/admin/auth/login', { method: 'POST', body })
export const getAdminProfile = () => request('/admin/auth/me')
export const logoutAdmin = () => request('/admin/auth/logout', { method: 'POST' })

export const getDashboardOverview = () => request('/admin/dashboard/overview')

export const listMerchants = (params) => request('/admin/merchants', { query: params })
export const getMerchantDetail = (id) => request(`/admin/merchants/${id}`)
export const updateMerchant = (id, body) => request(`/admin/merchants/${id}`, { method: 'PUT', body })
export const updateMerchantStatus = (id, status) => request(`/admin/merchants/${id}/status`, { method: 'PATCH', query: { status } })

export const listProviders = (params) => request('/admin/providers', { query: params })
export const createProvider = (body) => request('/admin/providers', { method: 'POST', body })
export const updateProvider = (id, body) => request(`/admin/providers/${id}`, { method: 'PUT', body })
export const updateProviderStatus = (id, status) => request(`/admin/providers/${id}/status`, { method: 'PATCH', query: { status } })
export const deleteProvider = (id) => request(`/admin/providers/${id}`, { method: 'DELETE' })

export const listProviderTokens = (params) => request('/admin/provider-tokens', { query: params })
export const createProviderToken = (body) => request('/admin/provider-tokens', { method: 'POST', body })
export const updateProviderToken = (id, body) => request(`/admin/provider-tokens/${id}`, { method: 'PUT', body })
export const updateProviderTokenStatus = (id, status) => request(`/admin/provider-tokens/${id}/status`, { method: 'PATCH', query: { status } })
export const deleteProviderToken = (id) => request(`/admin/provider-tokens/${id}`, { method: 'DELETE' })

export const listCustomerTokens = (params) => request('/admin/customer-tokens', { query: params })
export const createCustomerToken = (body) => request('/admin/customer-tokens', { method: 'POST', body })
export const updateCustomerToken = (id, body) => request(`/admin/customer-tokens/${id}`, { method: 'PUT', body })
export const updateCustomerTokenStatus = (id, status) => request(`/admin/customer-tokens/${id}/status`, { method: 'PATCH', query: { status } })
export const deleteCustomerToken = (id) => request(`/admin/customer-tokens/${id}`, { method: 'DELETE' })

export const listModels = (params) => request('/admin/models', { query: params })
export const createModel = (body) => request('/admin/models', { method: 'POST', body })
export const updateModel = (id, body) => request(`/admin/models/${id}`, { method: 'PUT', body })
export const updateModelStatus = (id, status) => request(`/admin/models/${id}/status`, { method: 'PATCH', query: { status } })
export const deleteModel = (id) => request(`/admin/models/${id}`, { method: 'DELETE' })

export const listProviderModels = (params) => request('/admin/provider-models', { query: params })
export const createProviderModel = (body) => request('/admin/provider-models', { method: 'POST', body })
export const updateProviderModel = (id, body) => request(`/admin/provider-models/${id}`, { method: 'PUT', body })
export const updateProviderModelStatus = (id, status) => request(`/admin/provider-models/${id}/status`, { method: 'PATCH', query: { status } })
export const deleteProviderModel = (id) => request(`/admin/provider-models/${id}`, { method: 'DELETE' })

export const listPlans = (params) => request('/admin/plans', { query: params })
export const createPlan = (body) => request('/admin/plans', { method: 'POST', body })
export const updatePlan = (id, body) => request(`/admin/plans/${id}`, { method: 'PUT', body })
export const updatePlanStatus = (id, status) => request(`/admin/plans/${id}/status`, { method: 'PATCH', query: { status } })
export const updatePlanVisible = (id, visible) => request(`/admin/plans/${id}/visible`, { method: 'PATCH', query: { visible } })
export const deletePlan = (id) => request(`/admin/plans/${id}`, { method: 'DELETE' })

export const listSubscriptions = (params) => request('/admin/subscriptions', { query: params })
export const getSubscriptionDetail = (id) => request(`/admin/subscriptions/${id}`)

export const listActivationCodes = (params) => request('/admin/activation-codes', { query: params })
export const generateActivationCodes = (body) => request('/admin/activation-codes/generate', { method: 'POST', body })
export const updateActivationCode = (id, body) => request(`/admin/activation-codes/${id}`, { method: 'PUT', body })
export const updateActivationCodeStatus = (id, status) => request(`/admin/activation-codes/${id}/status`, { method: 'PATCH', query: { status } })
export const deleteActivationCode = (id) => request(`/admin/activation-codes/${id}`, { method: 'DELETE' })

export const listPaymentMethods = (params) => request('/admin/payment-methods', { query: params })
export const createPaymentMethod = (body) => request('/admin/payment-methods', { method: 'POST', body })
export const getPaymentMethodDetail = (id) => request(`/admin/payment-methods/${id}`)
export const updatePaymentMethod = (id, body) => request(`/admin/payment-methods/${id}`, { method: 'PUT', body })
export const updatePaymentMethodStatus = (id, status) => request(`/admin/payment-methods/${id}/status`, { method: 'PATCH', query: { status } })
export const deletePaymentMethod = (id) => request(`/admin/payment-methods/${id}`, { method: 'DELETE' })

export const listPayLinks = (params) => request('/admin/pay-links', { query: params })
export const createPayLink = (body) => request('/admin/pay-links', { method: 'POST', body })
export const getPayLinkDetail = (id) => request(`/admin/pay-links/${id}`)
export const updatePayLink = (id, body) => request(`/admin/pay-links/${id}`, { method: 'PUT', body })
export const updatePayLinkStatus = (id, status) => request(`/admin/pay-links/${id}/status`, { method: 'PATCH', query: { status } })
export const deletePayLink = (id) => request(`/admin/pay-links/${id}`, { method: 'DELETE' })
