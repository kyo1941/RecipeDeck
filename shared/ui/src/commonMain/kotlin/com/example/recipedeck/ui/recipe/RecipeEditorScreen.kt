package com.example.recipedeck.ui.recipe

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recipedeck.domain.amount.AmountUnit
import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.ui.CenteredContent
import com.example.recipedeck.ui.LoadFailedContent
import com.example.recipedeck.ui.amount.unitText
import com.example.recipedeck.ui.navigation.ModalBackHandler
import com.example.recipedeck.ui.navigation.ModalCloseButton
import com.example.recipedeck.ui.resources.Res
import com.example.recipedeck.ui.resources.leftover_edit
import com.example.recipedeck.ui.resources.leftover_reflect
import com.example.recipedeck.ui.resources.recipe_editor_add_ingredient
import com.example.recipedeck.ui.resources.recipe_editor_discard
import com.example.recipedeck.ui.resources.recipe_editor_discard_title
import com.example.recipedeck.ui.resources.recipe_editor_draft
import com.example.recipedeck.ui.resources.recipe_editor_fraction_slash
import com.example.recipedeck.ui.resources.recipe_editor_ingredient_name
import com.example.recipedeck.ui.resources.recipe_editor_ingredient_name_required
import com.example.recipedeck.ui.resources.recipe_editor_ingredients
import com.example.recipedeck.ui.resources.recipe_editor_keep_editing
import com.example.recipedeck.ui.resources.recipe_editor_memo
import com.example.recipedeck.ui.resources.recipe_editor_name
import com.example.recipedeck.ui.resources.recipe_editor_name_required
import com.example.recipedeck.ui.resources.recipe_editor_quantity
import com.example.recipedeck.ui.resources.recipe_editor_remove_ingredient
import com.example.recipedeck.ui.resources.recipe_editor_save
import com.example.recipedeck.ui.resources.recipe_editor_save_failed
import com.example.recipedeck.ui.resources.recipe_editor_title_create
import com.example.recipedeck.ui.resources.recipe_editor_title_edit
import com.example.recipedeck.ui.resources.recipe_editor_unit_none
import com.example.recipedeck.ui.resources.recipe_load_failed
import com.example.recipedeck.ui.resources.recipe_no_longer_exists
import com.example.recipedeck.ui.resources.storage_shortage_hint
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource

@Composable
fun CreateRecipeScreen(viewModel: CreateRecipeViewModel, onClosed: () -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is RecipeEditorStep.Finished) onClosed()
    }
    val editor = (uiState as? RecipeEditorStep.Editing)?.editor
    // エディタがない間は、閉じても失われる入力も残った編集もないので、ViewModel を通さずに閉じる。
    val onCloseRequest = if (editor != null) viewModel::requestClose else onClosed
    ModalBackHandler(onCloseRequest = onCloseRequest)

    RecipeEditorFrame(
        title = stringResource(Res.string.recipe_editor_title_create),
        editor = editor,
        actions = viewModel,
        onCloseRequest = onCloseRequest,
        modifier = modifier,
    ) { contentModifier ->
        when (val state = uiState) {
            CreateRecipeUiState.Loading, is RecipeEditorStep.Finished -> CenteredContent(contentModifier) { CircularProgressIndicator() }
            is RecipeEditorStep.Editing -> RecipeEditorForm(state.editor, viewModel, contentModifier)
        }
    }
}

@Composable
fun EditRecipeScreen(viewModel: EditRecipeViewModel, onClosed: () -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is RecipeEditorStep.Finished) onClosed()
    }
    val editor = (uiState as? RecipeEditorStep.Editing)?.editor
    // エディタがない間は、閉じても失われる入力も残った編集もないので、ViewModel を通さずに閉じる。
    val onCloseRequest = if (editor != null) viewModel::requestClose else onClosed
    ModalBackHandler(onCloseRequest = onCloseRequest)

    RecipeEditorFrame(
        title = stringResource(Res.string.recipe_editor_title_edit),
        editor = editor,
        actions = viewModel,
        onCloseRequest = onCloseRequest,
        modifier = modifier,
    ) { contentModifier ->
        when (val state = uiState) {
            EditRecipeUiState.Loading, is RecipeEditorStep.Finished -> CenteredContent(contentModifier) { CircularProgressIndicator() }
            EditRecipeUiState.LoadFailed -> LoadFailedContent(stringResource(Res.string.recipe_load_failed), viewModel::retry, contentModifier)
            EditRecipeUiState.NoLongerExists -> CenteredContent(contentModifier) { Text(stringResource(Res.string.recipe_no_longer_exists)) }
            is RecipeEditorStep.Editing -> RecipeEditorForm(state.editor, viewModel, contentModifier)
        }
    }
}

@Composable
private fun RecipeEditorFrame(
    title: String,
    editor: RecipeEditorUiState?,
    actions: RecipeEditorActions,
    onCloseRequest: () -> Unit,
    modifier: Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { ModalCloseButton(onCloseRequest = onCloseRequest, enabled = editor?.saveStatus != SaveStatus.Saving) },
                actions = {
                    if (editor != null) {
                        TextButton(onClick = actions::save, enabled = editor.saveStatus != SaveStatus.Saving) {
                            Text(stringResource(Res.string.recipe_editor_save))
                        }
                    }
                },
            )
        },
    ) { innerPadding -> content(Modifier.padding(innerPadding)) }

    if (editor?.showsDiscardConfirmation == true) {
        AlertDialog(
            onDismissRequest = actions::keepEditing,
            title = { Text(stringResource(Res.string.recipe_editor_discard_title)) },
            confirmButton = { TextButton(onClick = actions::discard) { Text(stringResource(Res.string.recipe_editor_discard)) } },
            dismissButton = { TextButton(onClick = actions::keepEditing) { Text(stringResource(Res.string.recipe_editor_keep_editing)) } },
        )
    }
}

@Composable
private fun RecipeEditorForm(editor: RecipeEditorUiState, actions: RecipeEditorActions, modifier: Modifier) {
    // NOTE: UI を作り終えるまでのメモ。画面を書き直すときの、状態の置き場所の判断に使う。
    // データや保存の規則に関わる状態は ViewModel に置く（閉じるときの確認のダイアログはこちら。「変わっていれば確かめる」をテストで守るため）。
    // フォーカスのように UI の仕組みが持っていて、データに効かない状態は画面に置く。
    // ViewModel にも持つと、キーボードを閉じたときなどの変化を知らせ漏らし、実際のフォーカスとずれるため。
    // 「/」のボタンや Item の候補を出す行は後者なので、ここで持つ。
    var focusedQuantityKey by remember { mutableStateOf<Long?>(null) }
    var focusedNameKey by remember { mutableStateOf<Long?>(null) }
    val isEditable = editor.saveStatus != SaveStatus.Saving
    val ingredientKeysRequiringName = editor.ingredientKeysRequiringName
    val shownFoundItems = editor.shownFoundItems

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (editor.offeredLeftover != null) {
            item {
                LeftoverEditBanner(
                    message = stringResource(Res.string.leftover_edit),
                    useLabel = stringResource(Res.string.leftover_reflect),
                    onUse = actions::reflectLeftover,
                    onDiscard = actions::discardLeftover,
                    enabled = isEditable,
                )
            }
        }
        if (editor.saveStatus == SaveStatus.Failed) {
            item { SaveFailedMessage() }
        }
        item {
            OutlinedTextField(
                value = editor.form.name,
                onValueChange = { actions.changeName(it) },
                enabled = isEditable,
                label = { Text(stringResource(Res.string.recipe_editor_name)) },
                isError = editor.isNameRequired,
                supportingText = if (editor.isNameRequired) {
                    { Text(stringResource(Res.string.recipe_editor_name_required)) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(Res.string.recipe_editor_draft))
                Switch(checked = editor.form.isDraft, onCheckedChange = { actions.changeDraft(it) }, enabled = isEditable)
            }
        }
        item {
            Text(
                stringResource(Res.string.recipe_editor_ingredients),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        items(editor.form.ingredients.rows, key = { it.key }) { row ->
            IngredientRow(
                key = row.key,
                ingredient = row.ingredient,
                isNameRequired = row.key in ingredientKeysRequiringName,
                foundItems = if (focusedNameKey == row.key && shownFoundItems?.query?.ingredientKey == row.key) {
                    shownFoundItems.items.take(MAX_SHOWN_FOUND_ITEMS).toImmutableList()
                } else {
                    persistentListOf()
                },
                isQuantityFocused = focusedQuantityKey == row.key,
                isEditable = isEditable,
                onNameFocusChange = { focused ->
                    focusedNameKey = if (focused) row.key else focusedNameKey.takeUnless { it == row.key }
                },
                onQuantityFocusChange = { focused ->
                    focusedQuantityKey = if (focused) row.key else focusedQuantityKey.takeUnless { it == row.key }
                },
                actions = actions,
            )
        }
        item {
            TextButton(onClick = { actions.addIngredient() }, enabled = isEditable, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(stringResource(Res.string.recipe_editor_add_ingredient))
            }
        }
        item {
            OutlinedTextField(
                value = editor.form.memo,
                onValueChange = { actions.changeMemo(it) },
                enabled = isEditable,
                label = { Text(stringResource(Res.string.recipe_editor_memo)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun SaveFailedMessage() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(stringResource(Res.string.recipe_editor_save_failed), color = MaterialTheme.colorScheme.error)
        Text(stringResource(Res.string.storage_shortage_hint), color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun IngredientRow(
    key: Long,
    ingredient: IngredientForm,
    isNameRequired: Boolean,
    foundItems: ImmutableList<Item>,
    isQuantityFocused: Boolean,
    isEditable: Boolean,
    onNameFocusChange: (Boolean) -> Unit,
    onQuantityFocusChange: (Boolean) -> Unit,
    actions: RecipeEditorActions,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        IngredientNameField(
            name = ingredient.name,
            foundItems = foundItems,
            isNameRequired = isNameRequired,
            isEditable = isEditable,
            onNameChange = { actions.changeIngredientName(key, it) },
            onItemSelect = { actions.selectItem(key, it) },
            onFocusChange = onNameFocusChange,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = ingredient.quantity,
                onValueChange = { actions.changeQuantity(key, it) },
                enabled = isEditable,
                label = { Text(stringResource(Res.string.recipe_editor_quantity)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f).onFocusChanged { onQuantityFocusChange(it.isFocused) },
            )
            if (isQuantityFocused) {
                TextButton(onClick = { actions.insertFractionSlash(key) }, enabled = isEditable) {
                    Text(stringResource(Res.string.recipe_editor_fraction_slash))
                }
            }
            UnitSelector(unit = ingredient.unit, enabled = isEditable, onUnitChange = { actions.changeUnit(key, it) })
            TextButton(onClick = { actions.removeIngredient(key) }, enabled = isEditable) {
                Text(stringResource(Res.string.recipe_editor_remove_ingredient))
            }
        }
    }
}

@Composable
private fun IngredientNameField(
    name: String,
    foundItems: ImmutableList<Item>,
    isNameRequired: Boolean,
    isEditable: Boolean,
    onNameChange: (String) -> Unit,
    onItemSelect: (Item) -> Unit,
    onFocusChange: (Boolean) -> Unit,
) {
    Column {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            enabled = isEditable,
            label = { Text(stringResource(Res.string.recipe_editor_ingredient_name)) },
            isError = isNameRequired,
            supportingText = if (isNameRequired) {
                { Text(stringResource(Res.string.recipe_editor_ingredient_name_required)) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().onFocusChanged { onFocusChange(it.isFocused) },
        )
        // 候補を欄の上に重ねると、すぐ下の数量の欄を覆って押せなくなるので、欄の下に差し込む。
        if (foundItems.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(FOUND_ITEMS_BAND_HEIGHT).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                foundItems.forEach { item ->
                    SuggestionChip(onClick = { onItemSelect(item) }, label = { Text(item.displayName.value) })
                }
            }
        }
    }
}

@Composable
private fun UnitSelector(unit: AmountUnit?, enabled: Boolean, onUnitChange: (AmountUnit?) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { isExpanded = true }, enabled = enabled) { Text(unitLabel(unit)) }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            (listOf(null) + AmountUnit.entries).forEach { option ->
                DropdownMenuItem(
                    text = { Text(unitLabel(option)) },
                    onClick = {
                        onUnitChange(option)
                        isExpanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun unitLabel(unit: AmountUnit?): String = unit?.let { unitText(it) } ?: stringResource(Res.string.recipe_editor_unit_none)

private const val MAX_SHOWN_FOUND_ITEMS = 5
private val FOUND_ITEMS_BAND_HEIGHT = 48.dp
