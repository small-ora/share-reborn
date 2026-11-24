package io.github.smallora.sharereborn.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.smallora.sharereborn.BuildConfig
import io.github.smallora.sharereborn.data.model.WeiboStatus
import io.github.smallora.sharereborn.ui.detail.DetailScreen
import io.github.smallora.sharereborn.ui.detail.DetailViewModel
import io.github.smallora.sharereborn.ui.home.HomeFeed
import io.github.smallora.sharereborn.ui.home.HomeViewModel
import kotlinx.coroutines.launch

private val TABS = listOf("微博", "消息", "热门")

private val DRAWER_ITEMS = listOf(
    "我的评论和赞", "收藏", "我的关注", "超话社区", "附近的微博",
    "浏览历史", "扫一扫", "草稿箱", "主题风格", "设置",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val homeVm: HomeViewModel = viewModel()
    val homeState by homeVm.state.collectAsState()

    var tab by remember { mutableIntStateOf(0) }
    var openedStatus by remember { mutableStateOf<WeiboStatus?>(null) }
    var gateFeature by remember { mutableStateOf<String?>(null) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { homeVm.loadInitial() }
    BackHandler(enabled = openedStatus != null) { openedStatus = null }

    if (openedStatus != null) {
        val detailVm: DetailViewModel = viewModel()
        val detailState by detailVm.state.collectAsState()
        LaunchedEffect(openedStatus) {
            openedStatus?.let { detailVm.load(it.mblogid, it) }
        }
        DetailScreen(
            state = detailState,
            onBack = { openedStatus = null },
            onLoadMore = { detailVm.loadMoreComments() },
            onActionGate = { gateFeature = it },
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Column(Modifier.padding(20.dp)) {
                            Text("游客模式", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "未登录，仅可浏览",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider()
                        DRAWER_ITEMS.forEach { item ->
                            NavigationDrawerItem(
                                label = { Text(item) },
                                selected = false,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    gateFeature = item
                                },
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                        HorizontalDivider()
                        Text(
                            "版本 ${BuildConfig.VERSION_NAME} · 构建 ${BuildConfig.VERSION_CODE}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                }
            },
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(TABS[tab]) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "菜单")
                            }
                        },
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { gateFeature = "发布微博" }) {
                        Icon(Icons.Filled.Add, contentDescription = "发布")
                    }
                },
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    TabRow(selectedTabIndex = tab) {
                        TABS.forEachIndexed { index, title ->
                            Tab(
                                selected = tab == index,
                                onClick = { tab = index },
                                text = { Text(title) },
                            )
                        }
                    }
                    when (tab) {
                        0 -> HomeFeed(
                            state = homeState,
                            onRefresh = { homeVm.refresh() },
                            onLoadMore = { homeVm.loadMore() },
                            onRetry = { homeVm.loadInitial() },
                            onOpenDetail = { openedStatus = it },
                            onActionGate = { gateFeature = it },
                        )

                        else -> PlaceholderTab(TABS[tab])
                    }
                }
            }
        }
    }

    gateFeature?.let { feature ->
        LoginGateDialog(feature = feature, onDismiss = { gateFeature = null })
    }
}

@Composable
private fun PlaceholderTab(title: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            "该模块将在后续版本实现",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
