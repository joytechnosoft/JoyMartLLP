package com.joymart.llp.data

import com.google.gson.annotations.SerializedName

data class ApiEnvelope<T>(val success: Boolean = false, val status: Boolean = false, val message: String? = null, val data: T? = null, val user: UserDto? = null, val token: String? = null, val role: String? = null, @SerializedName("is_admin") val isAdmin: Boolean = false)
data class UserDto(
    val id: Long = 0, @SerializedName("member_id") val memberId: String? = null, val name: String = "", val email: String? = null, val phone: String? = null,
    val role: String? = null, @SerializedName("role_master") val roleMaster: RoleDto? = null,
    val status: String? = null, @SerializedName("is_active_member") val isActiveMember: Int = 0,
    @SerializedName("referral_code") val referralCode: String? = null,
    @SerializedName("wallet_balance") val walletBalance: String? = null
)
data class RoleDto(val name: String? = null, @SerializedName("display_name") val displayName: String? = null)
data class LoginRequest(val login: String, val password: String)
data class RegisterRequest(
    val name: String,
    val phone: String,
    val email: String?,
    val password: String,
    @SerializedName("password_confirmation") val passwordConfirmation: String,
    @SerializedName("parent_id") val parentId: String,
    val role: String,
    val referral_code: String? = null
)
data class AllowedRoleDto(
    val name: String = "",
    @SerializedName("display_name") val displayName: String = "",
    @SerializedName("level_order") val levelOrder: Int = 0
)
data class SponsorCheckDto(
    val success: Boolean = false,
    val message: String? = null,
    @SerializedName("sponsor_name") val sponsorName: String? = null,
    @SerializedName("sponsor_role") val sponsorRole: String? = null,
    @SerializedName("allowed_roles") val allowedRoles: List<AllowedRoleDto> = emptyList()
)
data class BootstrapDto(val user: UserDto? = null, @SerializedName("is_admin") val isAdmin: Boolean = false, val modules: List<String> = emptyList())
data class ProductDto(val id: Long, @SerializedName("product_code") val code: String, @SerializedName("product_name") val name: String, val unit: String? = null, @SerializedName("sell_price") val sellPrice: String? = null, @SerializedName("stock_qty") val stockQty: String? = null)
data class PackageDto(val id: Long, @SerializedName("package_code") val code: String, @SerializedName("package_name") val name: String, @SerializedName("package_price") val price: String, val status: String? = null, @SerializedName("total_cost") val totalCost: String? = null, @SerializedName("commission_pool_amount") val commissionPool: String? = null)
data class CategoryDto(val id: Long, @SerializedName("category_name") val name: String)
data class VrindabanList<T>(val data: List<T> = emptyList(), val current_page: Int? = null, val last_page: Int? = null)
data class NativePackageItem(val product_id: Long, val quantity: Double)
data class NativePackageRequest(
    val package_code: String, val package_name: String, val description: String?, val package_price: Double,
    val renewal_price: Double?, val validity_days: Int?, val commission_pool_percent: Double,
    val is_membership_product: Boolean, val status: String, val items: List<NativePackageItem>,
    val software_ids: List<Long> = emptyList(), val software_access: Map<String, String> = emptyMap(),
    val membership_package_ids: List<Long> = emptyList(), val levels: Map<String, Double> = emptyMap()
)
data class NativeProductRequest(val product_code: String, val product_name: String, val description: String?, val category_id: Long?, val unit: String, val base_price: Double, val mrp: Double, val sell_price: Double, val stock_qty: Double, val low_stock_qty: Double, val status: String = "ACTIVE")
