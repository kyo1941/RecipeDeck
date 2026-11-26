package com.example.recipedeck.ui.recipe

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.recipedeck.domain.amount.AmountUnit
import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.domain.item.ItemId
import com.example.recipedeck.domain.recipe.RecipeStatus
import com.example.recipedeck.ui.amount.isAcceptableQuantityText
import com.example.recipedeck.ui.amount.notationOf
import com.example.recipedeck.ui.amount.quantityOrNull
import com.example.recipedeck.ui.amount.editableText
import com.example.recipedeck.usecase.ingredient.IngredientInput
import com.example.recipedeck.usecase.ingredient.ItemInput
import com.example.recipedeck.usecase.recipe.ObserveRecipeResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

data class RecipeForm(
    val name: String,
    val isDraft: Boolean,
    val memo: String,
    val ingredients: IngredientListForm,
) {
    val status: RecipeStatus get() = if (isDraft) RecipeStatus.DRAFT else RecipeStatus.READY

    fun withIngredientChanged(key: Long, transform: (IngredientForm) -> IngredientForm): RecipeForm =
        copy(ingredients = ingredients.updated(key, transform))

    fun withIngredientAdded(): RecipeForm = copy(ingredients = ingredients.added())

    fun withIngredientRemoved(key: Long): RecipeForm = copy(ingredients = ingredients.removed(key))

    fun toLeftoverEdit() = LeftoverRecipeEdit(
        name = name,
        isDraft = isDraft,
        memo = memo,
        ingredients = ingredients.rows.map { it.ingredient.toLeftoverEdit() },
    )

    fun hasSameContentAs(other: RecipeForm): Boolean =
        name == other.name &&
            isDraft == other.isDraft &&
            memo == other.memo &&
            ingredients.hasSameContentAs(other.ingredients)

    companion object {
        fun empty() = RecipeForm(name = "", isDraft = false, memo = "", ingredients = IngredientListForm.of(listOf(IngredientForm.empty())))

        fun of(found: ObserveRecipeResult.Found) = RecipeForm(
            name = found.recipe.name.value,
            isDraft = found.recipe.status == RecipeStatus.DRAFT,
            memo = found.recipe.memo?.value.orEmpty(),
            ingredients = IngredientListForm.of(found.ingredients.map(IngredientForm::of)),
        )

        fun of(leftover: LeftoverRecipeEdit) = RecipeForm(
            name = leftover.name,
            isDraft = leftover.isDraft,
            memo = leftover.memo,
            ingredients = IngredientListForm.of(leftover.ingredients.map(IngredientForm::of)),
        )
    }
}

@ConsistentCopyVisibility
data class IngredientListForm private constructor(
    val rows: ImmutableList<Row>,
    private val nextKey: Long,
) {
    data class Row(val key: Long, val ingredient: IngredientForm)

    fun added(): IngredientListForm = copy(rows = (rows + Row(nextKey, IngredientForm.empty())).toImmutableList(), nextKey = nextKey + 1)

    fun removed(key: Long): IngredientListForm = copy(rows = rows.filterNot { it.key == key }.toImmutableList())

    fun updated(key: Long, transform: (IngredientForm) -> IngredientForm): IngredientListForm =
        copy(rows = rows.map { if (it.key == key) it.copy(ingredient = transform(it.ingredient)) else it }.toImmutableList())

    fun toInputs(): List<IngredientInput> = rows.map { it.ingredient.toInput() }

    fun hasSameContentAs(other: IngredientListForm): Boolean =
        rows.size == other.rows.size && rows.zip(other.rows).all { (row, otherRow) -> row.ingredient.hasSameContentAs(otherRow.ingredient) }

    companion object {
        fun of(ingredients: List<IngredientForm>) = IngredientListForm(
            rows = ingredients.mapIndexed { index, ingredient -> Row(index.toLong(), ingredient) }.toImmutableList(),
            nextKey = ingredients.size.toLong(),
        )
    }
}

data class IngredientForm(
    val name: String,
    val selectedItemId: ItemId?,
    val quantity: TextFieldValue,
    val unit: AmountUnit?,
) {
    fun withName(name: String): IngredientForm = if (name == this.name) this else copy(name = name, selectedItemId = null)

    fun withItemSelected(item: Item): IngredientForm = copy(name = item.displayName.value, selectedItemId = item.id)

    fun withQuantity(quantity: TextFieldValue): IngredientForm {
        val accepted = quantity.text.filter { it in QUANTITY_CHARACTERS }
        return when {
            !isAcceptableQuantityText(accepted) -> this
            accepted == quantity.text -> copy(quantity = quantity)
            else -> copy(quantity = TextFieldValue(accepted, TextRange(accepted.length)))
        }
    }

    fun withFractionSlash(): IngredientForm {
        val inserted = quantity.text.replaceRange(quantity.selection.min, quantity.selection.max, "/")
        return if (isAcceptableQuantityText(inserted)) copy(quantity = TextFieldValue(inserted, TextRange(quantity.selection.min + 1))) else this
    }

    fun withUnit(unit: AmountUnit?): IngredientForm = copy(unit = unit)

    fun toInput() = IngredientInput(
        item = selectedItemId?.let(ItemInput::Selected) ?: ItemInput.Typed(name),
        quantity = quantityOrNull(quantity.text),
        notation = notationOf(quantity.text),
        unit = unit,
    )

    fun toLeftoverEdit() = LeftoverRecipeEdit.Ingredient(
        name = name,
        selectedItemId = selectedItemId?.value,
        quantity = quantity.text,
        unit = unit,
    )

    fun hasSameContentAs(other: IngredientForm): Boolean =
        name == other.name && selectedItemId == other.selectedItemId && quantity.text == other.quantity.text && unit == other.unit

    companion object {
        private const val QUANTITY_CHARACTERS = "0123456789./"

        fun empty() = IngredientForm(name = "", selectedItemId = null, quantity = TextFieldValue(""), unit = null)

        fun of(found: ObserveRecipeResult.Found.Ingredient) = IngredientForm(
            name = found.item.displayName.value,
            selectedItemId = found.item.id,
            quantity = TextFieldValue(found.amount?.let { it.quantity.editableText(it.notation) }.orEmpty()),
            unit = found.amount?.unit,
        )

        fun of(leftover: LeftoverRecipeEdit.Ingredient) = IngredientForm(
            name = leftover.name,
            selectedItemId = leftover.selectedItemId?.let(::ItemId),
            quantity = TextFieldValue(leftover.quantity),
            unit = leftover.unit,
        )
    }
}
