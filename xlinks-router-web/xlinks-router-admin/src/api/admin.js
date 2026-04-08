import { request } from '@/utils/http'

export const loginAdmin = (body) => request('/api/auth/login', { method: 'POST', body })
export const getAdminProfile = () => request('/api/auth/me')
export const logoutAdmin = () => request('/api/auth/logout', { method: 'POST' })

export const getDashboardOverview = () => request('/api/dashboard/overview')

export const listMerchants = (params) => request('/api/merchants', { query: params })
export const getMerchantDetail = (id) => request(`/api/merchants/${id}`)
export const updateMerchant = (id, body) => request(`/api/merchants/${id}`, { method: 'PUT', body })
export const updateMerchantStatus = (id, status) => request(`/api/merchants/${id}/status`, { method: 'PATCH', query: { status } })

export const listProviders = (params) => request('/api/providers', { query: params })
export const createProvider = (body) => request('/api/providers', { method: 'POST', body })
export const updateProvider = (id, body) => request(`/api/providers/${id}`, { method: 'PUT', body })
export const updateProviderStatus = (id, status) => request(`/api/providers/${id}/status`, { method: 'PATCH', query: { status } })
export const deleteProvider = (id) => request(`/api/providers/${id}`, { method: 'DELETE' })

export const listProviderTokens = (params) => request('/api/provider-tokens', { query: params })
export const createProviderToken = (body) => request('/api/provider-tokens', { method: 'POST', body })
export const updateProviderToken = (id, body) => request(`/api/provider-tokens/${id}`, { method: 'PUT', body })
export const updateProviderTokenStatus = (id, status) => request(`/api/provider-tokens/${id}/status`, { method: 'PATCH', query: { status } })
export const deleteProviderToken = (id) => request(`/api/provider-tokens/${id}`, { method: 'DELETE' })

export const listCustomerTokens = (params) => request('/api/customer-tokens', { query: params })
export const createCustomerToken = (body) => request('/api/customer-tokens', { method: 'POST', body })
export const updateCustomerToken = (id, body) => request(`/api/customer-tokens/${id}`, { method: 'PUT', body })
export const updateCustomerTokenStatus = (id, status) => request(`/api/customer-tokens/${id}/status`, { method: 'PATCH', query: { status } })
export const deleteCustomerToken = (id) => request(`/api/customer-tokens/${id}`, { method: 'DELETE' })

export const listModels = (params) => request('/api/models', { query: params })
export const createModel = (body) => request('/api/models', { method: 'POST', body })
export const updateModel = (id, body) => request(`/api/models/${id}`, { method: 'PUT', body })
export const updateModelStatus = (id, status) => request(`/api/models/${id}/status`, { method: 'PATCH', query: { status } })
export const deleteModel = (id) => request(`/api/models/${id}`, { method: 'DELETE' })

export const listProviderModels = (params) => request('/api/provider-models', { query: params })
export const createProviderModel = (body) => request('/api/provider-models', { method: 'POST', body })
export const updateProviderModel = (id, body) => request(`/api/provider-models/${id}`, { method: 'PUT', body })
export const updateProviderModelStatus = (id, status) => request(`/api/provider-models/${id}/status`, { method: 'PATCH', query: { status } })
export const deleteProviderModel = (id) => request(`/api/provider-models/${id}`, { method: 'DELETE' })

export const listPlans = (params) => request('/api/plans', { query: params })
export const createPlan = (body) => request('/api/plans', { method: 'POST', body })
export const updatePlan = (id, body) => request(`/api/plans/${id}`, { method: 'PUT', body })
export const updatePlanStatus = (id, status) => request(`/api/plans/${id}/status`, { method: 'PATCH', query: { status } })
export const updatePlanVisible = (id, visible) => request(`/api/plans/${id}/visible`, { method: 'PATCH', query: { visible } })
export const deletePlan = (id) => request(`/api/plans/${id}`, { method: 'DELETE' })

export const listSubscriptions = (params) => request('/api/subscriptions', { query: params })
export const getSubscriptionDetail = (id) => request(`/api/subscriptions/${id}`)

export const listActivationCodes = (params) => request('/api/activation-codes', { query: params })
export const generateActivationCodes = (body) => request('/api/activation-codes/generate', { method: 'POST', body })
export const updateActivationCode = (id, body) => request(`/api/activation-codes/${id}`, { method: 'PUT', body })
export const updateActivationCodeStatus = (id, status) => request(`/api/activation-codes/${id}/status`, { method: 'PATCH', query: { status } })
export const deleteActivationCode = (id) => request(`/api/activation-codes/${id}`, { method: 'DELETE' })

export const listPaymentMethods = (params) => request('/api/payment-methods', { query: params })
export const createPaymentMethod = (body) => request('/api/payment-methods', { method: 'POST', body })
export const getPaymentMethodDetail = (id) => request(`/api/payment-methods/${id}`)
export const updatePaymentMethod = (id, body) => request(`/api/payment-methods/${id}`, { method: 'PUT', body })
export const updatePaymentMethodStatus = (id, status) => request(`/api/payment-methods/${id}/status`, { method: 'PATCH', query: { status } })
export const deletePaymentMethod = (id) => request(`/api/payment-methods/${id}`, { method: 'DELETE' })

export const listPayLinks = (params) => request('/api/pay-links', { query: params })
export const createPayLink = (body) => request('/api/pay-links', { method: 'POST', body })
export const getPayLinkDetail = (id) => request(`/api/pay-links/${id}`)
export const updatePayLink = (id, body) => request(`/api/pay-links/${id}`, { method: 'PUT', body })
export const updatePayLinkStatus = (id, status) => request(`/api/pay-links/${id}/status`, { method: 'PATCH', query: { status } })
export const deletePayLink = (id) => request(`/api/pay-links/${id}`, { method: 'DELETE' })
