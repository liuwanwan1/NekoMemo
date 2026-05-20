package mirujam.nekomemo.data.repository

import kotlinx.coroutines.flow.Flow
import mirujam.nekomemo.data.local.dao.CategoryDao
import mirujam.nekomemo.data.local.dao.QuestionBankDao
import mirujam.nekomemo.data.local.entity.CategoryEntity
import mirujam.nekomemo.domain.validator.DataValidator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val questionBankDao: QuestionBankDao
) {
    companion object {
        const val DEFAULT_CATEGORY_NAME = "GENERAL"
    }

    fun getAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    suspend fun getAllCategoriesSync(): List<CategoryEntity> =
        categoryDao.getAllCategoriesSync()

    suspend fun getCategoryById(id: Long): CategoryEntity? =
        categoryDao.getCategoryById(id)

    suspend fun getCategoryByName(name: String): CategoryEntity? =
        categoryDao.getCategoryByName(name)

    suspend fun isDefaultCategory(categoryId: Long): Boolean {
        val category = categoryDao.getCategoryById(categoryId) ?: return false
        return category.name == DEFAULT_CATEGORY_NAME
    }

    fun getDefaultCategoryName(): String = DEFAULT_CATEGORY_NAME

    suspend fun isReservedCategoryName(name: String): Boolean {
        return name.uppercase() == DEFAULT_CATEGORY_NAME
    }

    suspend fun addCategory(name: String): Result<Long> {
        val trimmedName = name.trim()
        if (!DataValidator.isCategoryValid(trimmedName)) {
            return Result.failure(IllegalArgumentException("Invalid category name"))
        }
        if (isReservedCategoryName(trimmedName)) {
            return Result.failure(IllegalArgumentException("Cannot use reserved category name"))
        }
        val existing = categoryDao.getCategoryByName(trimmedName)
        if (existing != null) {
            return Result.success(existing.id)
        }
        val id = categoryDao.insertCategory(CategoryEntity(name = trimmedName))
        return Result.success(id)
    }

    suspend fun updateCategory(oldName: String, newName: String): Result<Unit> {
        val trimmedNewName = newName.trim()
        if (!DataValidator.isCategoryValid(trimmedNewName)) {
            return Result.failure(IllegalArgumentException("Invalid category name"))
        }
        if (isReservedCategoryName(trimmedNewName)) {
            return Result.failure(IllegalArgumentException("Cannot use reserved category name"))
        }
        val existing = categoryDao.getCategoryByName(trimmedNewName)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Category name already exists"))
        }
        val oldCategory = categoryDao.getCategoryByName(oldName) ?: return Result.failure(IllegalArgumentException("Category not found"))
        questionBankDao.updateBanksCategory(oldName, trimmedNewName)
        categoryDao.updateCategory(oldCategory.copy(name = trimmedNewName))
        return Result.success(Unit)
    }

    suspend fun renameCategory(categoryId: Long, newName: String): Result<Unit> {
        val trimmedNewName = newName.trim()
        if (!DataValidator.isCategoryValid(trimmedNewName)) {
            return Result.failure(IllegalArgumentException("Invalid category name"))
        }
        if (isReservedCategoryName(trimmedNewName)) {
            return Result.failure(IllegalArgumentException("Cannot use reserved category name"))
        }
        val existingWithNewName = categoryDao.getCategoryByName(trimmedNewName)
        if (existingWithNewName != null && existingWithNewName.id != categoryId) {
            return Result.failure(IllegalArgumentException("Category name already exists"))
        }
        val category = categoryDao.getCategoryById(categoryId) ?: return Result.failure(IllegalArgumentException("Category not found"))
        if (category.name == DEFAULT_CATEGORY_NAME) {
            return Result.failure(IllegalArgumentException("Cannot rename default category"))
        }
        questionBankDao.updateBanksCategory(category.name, trimmedNewName)
        categoryDao.updateCategory(category.copy(name = trimmedNewName))
        return Result.success(Unit)
    }

    suspend fun deleteCategory(categoryId: Long): Result<Unit> {
        val category = categoryDao.getCategoryById(categoryId) ?: return Result.failure(IllegalArgumentException("Category not found"))
        if (category.name == DEFAULT_CATEGORY_NAME) {
            return Result.failure(IllegalStateException("Cannot delete default category"))
        }
        val bankCount = categoryDao.getBankCountByCategory(category.name)
        if (bankCount > 0) {
            return Result.failure(IllegalStateException("Cannot delete category with existing banks"))
        }
        categoryDao.deleteCategory(category)
        return Result.success(Unit)
    }

    suspend fun canDeleteCategory(categoryId: Long): Boolean {
        val category = categoryDao.getCategoryById(categoryId) ?: return false
        if (category.name == DEFAULT_CATEGORY_NAME) {
            return false
        }
        val bankCount = categoryDao.getBankCountByCategory(category.name)
        return bankCount == 0
    }

    suspend fun getBankCountByCategory(categoryName: String): Int =
        categoryDao.getBankCountByCategory(categoryName)

    suspend fun ensureDefaultCategory() {
        if (categoryDao.getCategoryCount() == 0) {
            categoryDao.insertCategory(CategoryEntity(name = DEFAULT_CATEGORY_NAME))
        }
    }
}
