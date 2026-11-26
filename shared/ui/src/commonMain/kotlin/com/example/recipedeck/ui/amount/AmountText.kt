package com.example.recipedeck.ui.amount

import androidx.compose.runtime.Composable
import com.example.recipedeck.domain.amount.Amount
import com.example.recipedeck.domain.amount.AmountUnit
import com.example.recipedeck.ui.resources.Res
import com.example.recipedeck.ui.resources.amount_unit_gram
import com.example.recipedeck.ui.resources.amount_unit_kilogram
import com.example.recipedeck.ui.resources.amount_unit_liter
import com.example.recipedeck.ui.resources.amount_unit_milliliter
import com.example.recipedeck.ui.resources.amount_unit_piece
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun amountText(amount: Amount): String =
    amount.quantity.text(amount.notation) + amount.unit?.let { unitText(it) }.orEmpty()

@Composable
internal fun unitText(unit: AmountUnit): String = stringResource(
    when (unit) {
        AmountUnit.PIECE -> Res.string.amount_unit_piece
        AmountUnit.GRAM -> Res.string.amount_unit_gram
        AmountUnit.KILOGRAM -> Res.string.amount_unit_kilogram
        AmountUnit.MILLILITER -> Res.string.amount_unit_milliliter
        AmountUnit.LITER -> Res.string.amount_unit_liter
    },
)
