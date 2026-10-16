/**
 * 管理端接口（用户管理 / 角色管理，对应文档 §5.1）
 */
import { http } from '@/utils/request'

export interface RoleOption {
  id: number
  name: string
  code: string
  description?: string
  status: number
  permissions: string[]
}

export interface UserItem {
  id: number
  username: string
  nickname: string
  email?: string
  avatar?: string
  phone?: string
  status: number
  lastLoginTime?: string
  createTime?: string
  roles: string[]
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface UserForm {
  username?: string
  nickname?: string
  email?: string
  phone?: string
  password?: string
  status?: number
  roleIds?: number[]
}

/** 分页查询用户 GET /api/admin/users */
export function pageUsersApi(params: {
  page: number
  size: number
  keyword?: string
  status?: number
}): Promise<PageResult<UserItem>> {
  return http.get<PageResult<UserItem>>('/admin/users', params)
}

/** 用户详情 GET /api/admin/users/{id} */
export function getUserDetailApi(id: number): Promise<UserItem> {
  return http.get<UserItem>(`/admin/users/${id}`)
}

/** 创建用户 POST /api/admin/users */
export function createUserApi(data: UserForm): Promise<void> {
  return http.post<void>('/admin/users', data)
}

/** 修改用户 PUT /api/admin/users/{id} */
export function updateUserApi(id: number, data: UserForm): Promise<void> {
  return http.put<void>(`/admin/users/${id}`, data)
}

/** 删除用户 DELETE /api/admin/users/{id} */
export function deleteUserApi(id: number): Promise<void> {
  return http.delete<void>(`/admin/users/${id}`)
}

/** 角色列表 GET /api/admin/roles（用户分配角色用） */
export function listRolesApi(): Promise<RoleOption[]> {
  return http.get<RoleOption[]>('/admin/roles')
}

export interface PermissionItem {
  id: number
  name: string
  code: string
  type?: number
  parentId?: number
}

export interface RoleForm {
  name: string
  code: string
  description?: string
  status?: number
  permissionIds?: number[]
}

/** 权限列表 GET /api/admin/permissions（角色管理页勾选权限） */
export function listPermissionsApi(): Promise<PermissionItem[]> {
  return http.get<PermissionItem[]>('/admin/permissions')
}

/** 创建角色 POST /api/admin/roles */
export function createRoleApi(data: RoleForm): Promise<void> {
  return http.post<void>('/admin/roles', data)
}

/** 修改角色 PUT /api/admin/roles/{id} */
export function updateRoleApi(id: number, data: RoleForm): Promise<void> {
  return http.put<void>(`/admin/roles/${id}`, data)
}

/** 删除角色 DELETE /api/admin/roles/{id} */
export function deleteRoleApi(id: number): Promise<void> {
  return http.delete<void>(`/admin/roles/${id}`)
}
