const { createApp, reactive, ref, computed, onMounted } = Vue;

const icon = (name) => `/icons/${name}`;
const pic = (name) => `/assets/pic/${name}`;

createApp({
  setup() {
    const state = reactive({
      playerName: '',
      money: 0,
      gems: 0,
      day: 1,
      autoTick: 0,
      totalIncome: 0,
      totalExpense: 0,
      homeLevel: 3,
      claimedAchievements: 0,
      pets: [],
      inventory: [],
      history: [],
      statusMessage: ''
    });

    const activeMenu = ref('家园');
    const activeInventoryTab = ref('全部');
    const currentTime = ref('');
    const selectedPetId = ref(null);
    const busyAction = ref('');
    const toast = ref('');
    const toastTimer = ref(null);
    const resetArmed = ref(false);
    const settings = reactive({
      sound: true,
      animation: true,
      autosave: true,
      compact: false
    });

    const menus = [
      { label: '家园', icon: icon('menu_home.png') },
      { label: '宠物', icon: icon('menu_pet.png') },
      { label: '训练', icon: icon('menu_train.png') },
      { label: '探险', icon: icon('menu_explore.png') },
      { label: '任务', icon: icon('menu_tasks.png') },
      { label: '商店', icon: icon('menu_shop.png') },
      { label: '图鉴', icon: icon('menu_album.png') },
      { label: '成就', icon: icon('menu_achievement.png') },
      { label: '好友', icon: icon('menu_friends.png') },
      { label: '规则', icon: icon('menu_tasks.png') },
      { label: '设置', icon: icon('menu_settings.png') }
    ];

    const inventoryTabs = ['全部', '食物', '玩具', '道具', '材料'];
    const gameRules = [
      {
        title: '经营目标',
        text: '照护狗狗和兔子等宠物，维持金币、库存和宠物状态，让家园稳定成长。'
      },
      {
        title: '宠物状态',
        text: '饱食度由饥饿值换算，越高越好；清洁度取决于健康；心情和亲密度会影响成长与售价。'
      },
      {
        title: '日常动作',
        text: '喂食消耗营养粮或金币，清洁消耗洗护液或金币，互动优先消耗玩具，休息恢复健康和心情。'
      },
      {
        title: '训练与探索',
        text: '训练需要金币，会提升心情、健康和亲密度；探索需要宠物状态良好，完成后带回金币并增加饥饿。'
      },
      {
        title: '商店与背包',
        text: '商店购买会扣除金币并增加对应库存；金币不足时不会购买，也不会让余额被扣到 0 以下。'
      },
      {
        title: '领养与出售',
        text: '领养需要支付宠物标价；出售会获得宠物标价加状态收益，但至少要保留一只宠物。'
      },
      {
        title: '福瑞形态',
        text: '宠物成长值达到 360 且亲密度达到 70 后，会从幼体成长为福瑞形态，主图和图鉴会切换为人形态资源。'
      }
    ];

    const pageMap = {
      '宠物': {
        title: '宠物管理',
        subtitle: '查看宠物状态，执行喂食、清洁、互动和卖出。',
        icon: icon('menu_pet.png'),
        stats: [
          { label: '宠物数量', value: () => state.pets.length },
          { label: '平均心情', value: () => `${Math.round(state.pets.reduce((sum, pet) => sum + pet.mood, 0) / Math.max(1, state.pets.length) || 0)}%` },
          { label: '今日消耗', value: () => `${formatNumber(state.totalExpense)} 金币` }
        ],
        sideTitle: '宠物概况',
        sideItems: [
          { label: '总宠物', value: () => state.pets.length },
          { label: '已领养', value: () => state.pets.filter(pet => pet.adopted).length },
          { label: '可操作', value: () => selectedPet.value ? 1 : 0 }
        ]
      },
      '训练': {
        title: '训练中心',
        subtitle: '通过训练提升活力、情绪与成长效率，成长值达标后会解锁福瑞形态。',
        icon: icon('menu_train.png'),
        stats: [
          { label: '课程数', value: () => trainingCourses.length },
          { label: '当前收益', value: () => `${formatNumber(todayIncome.value)} 金币` },
          { label: '状态', value: () => '可训练' }
        ],
        sideTitle: '训练摘要',
        sideItems: [
          { label: '推荐课程', value: () => trainingCourses[0].title },
          { label: '训练消耗', value: () => trainingCourses[0].cost },
          { label: '当前形态', value: () => petForm.value }
        ]
      },
      '探险': {
        title: '探索地图',
        subtitle: '派出宠物进入森林与村落，寻找稀有素材和奖励。',
        icon: icon('menu_explore.png'),
        stats: [
          { label: '地图数', value: () => exploreMissions.length },
          { label: '可领取', value: () => `${state.gems} 宝石` },
          { label: '状态', value: () => '开放' }
        ],
        sideTitle: '探索摘要',
        sideItems: [
          { label: '推荐路线', value: () => exploreMissions[0].title },
          { label: '预计奖励', value: () => exploreMissions[0].reward },
          { label: '完成度', value: () => `${Math.min(100, state.day * 8)}%` }
        ]
      },
      '任务': {
        title: '任务中心',
        subtitle: '按日计划完成喂食、互动、清洁与训练任务。',
        icon: icon('menu_tasks.png'),
        stats: [
          { label: '任务进度', value: () => `${completedPlans.value}/4` },
          { label: '今日奖励', value: () => `${todayIncome.value} 金币` },
          { label: '任务状态', value: () => '进行中' }
        ],
        sideTitle: '任务摘要',
        sideItems: [
          { label: '当前主线', value: () => taskBoard.value[0].label },
          { label: '下个奖励', value: () => taskBoard.value[0].reward },
          { label: '完成数', value: () => `${completedPlans.value}/4` }
        ]
      },
      '商店': {
        title: '补给商店',
        subtitle: '购买食物、玩具、清洁和材料，维持家园运转。',
        icon: icon('menu_shop.png'),
        stats: [
          { label: '余额', value: () => `${formatNumber(state.money)} 金币` },
          { label: '商品数', value: () => shopItems.length },
          { label: '库存', value: () => `${bagUsed.value}/${bagLimit.value}` }
        ],
        sideTitle: '商店摘要',
        sideItems: [
          { label: '热卖商品', value: () => shopItems[0].name },
          { label: '价格', value: () => `${shopItems[0].price} 金币` },
          { label: '补货状态', value: () => '供应稳定' }
        ]
      },
      '图鉴': {
        title: '宠物图鉴',
        subtitle: '记录已解锁的宠物、场景与收藏项。',
        icon: icon('menu_album.png'),
        stats: [
          { label: '条目数', value: () => albumItems.value.length },
          { label: '已解锁', value: () => `${albumItems.value.filter(item => item.state === '已收录').length}` },
          { label: '收藏度', value: () => `${Math.min(100, 30 + state.pets.length * 12)}%` }
        ],
        sideTitle: '图鉴摘要',
        sideItems: [
          { label: '最近解锁', value: () => selectedPet.value ? selectedPet.value.species : albumItems.value[0].name },
          { label: '未解锁', value: () => `${lockedAlbumCount.value}` },
          { label: '收藏目标', value: () => '全图鉴' }
        ]
      },
      '成就': {
        title: '成就进度',
        subtitle: '查看当前经营里程碑和累计奖励。',
        icon: icon('menu_achievement.png'),
        stats: [
          { label: '成就数', value: () => achievementItems.value.length },
          { label: '完成率', value: () => `${Math.round(achievementItems.value.reduce((sum, item) => sum + item.progress, 0) / achievementItems.value.length)}%` },
          { label: '奖励', value: () => `${claimableAchievements.value} 可领取` }
        ],
        sideTitle: '成就摘要',
        sideItems: [
          { label: '最高进度', value: () => `${Math.max(...achievementItems.value.map(item => item.progress))}%` },
          { label: '待完成', value: () => `${achievementItems.value.filter(item => item.progress < 100).length}` },
          { label: '总数', value: () => achievementItems.value.length }
        ]
      },
      '好友': {
        title: '好友小屋',
        subtitle: '浏览好友家园，互访并交换日常资源。',
        icon: icon('menu_friends.png'),
        stats: [
          { label: '好友数', value: () => friendItems.length },
          { label: '今日访客', value: () => `${state.day * 2}` },
          { label: '互动', value: () => '活跃' }
        ],
        sideTitle: '好友摘要',
        sideItems: [
          { label: '最近访问', value: () => friendItems[0].name },
          { label: '好友状态', value: () => friendItems[0].status },
          { label: '可拜访', value: () => friendItems.length }
        ]
      },
      '规则': {
        title: '游戏规则',
        subtitle: '了解基础循环、状态、商店、领养和探索的结算方式。',
        icon: icon('menu_tasks.png'),
        stats: [
          { label: '规则条目', value: () => gameRules.length },
          { label: '核心循环', value: () => '照护经营' },
          { label: '资源', value: () => '金币/库存' }
        ],
        sideTitle: '规则摘要',
        sideItems: [
          { label: '目标', value: () => '稳定经营' },
          { label: '关键状态', value: () => '饱食/清洁/心情' },
          { label: '失败保护', value: () => '余额校验' }
        ]
      },
      '设置': {
        title: '游戏设置',
        subtitle: '管理音效、画面、自动保存和重开行为。',
        icon: icon('menu_settings.png'),
        stats: [
          { label: '自动存档', value: () => settings.autosave ? '开启' : '关闭' },
          { label: '当前版本', value: () => 'v1.0' },
          { label: '运行状态', value: () => busyAction.value ? '处理中' : '正常' }
        ],
        sideTitle: '设置摘要',
        sideItems: [
          { label: '保存频率', value: () => settings.autosave ? '60 秒' : '手动' },
          { label: '重置入口', value: () => '可用' },
          { label: '动画', value: () => settings.animation ? '流畅' : '关闭' }
        ]
      }
    };

    const saveSettings = () => {
      localStorage.setItem('petgame-settings', JSON.stringify({
        sound: settings.sound,
        animation: settings.animation,
        autosave: settings.autosave,
        compact: settings.compact
      }));
    };

    const loadSettings = () => {
      try {
        const saved = JSON.parse(localStorage.getItem('petgame-settings') || '{}');
        Object.assign(settings, saved);
      } catch (error) {
        localStorage.removeItem('petgame-settings');
      }
    };

    const showToast = (message) => {
      toast.value = message || state.statusMessage || '操作已完成。';
      window.clearTimeout(toastTimer.value);
      toastTimer.value = window.setTimeout(() => {
        toast.value = '';
      }, 2600);
    };

    const playClick = () => {
      if (!settings.sound || !window.AudioContext && !window.webkitAudioContext) return;
      const AudioContextCtor = window.AudioContext || window.webkitAudioContext;
      const ctx = new AudioContextCtor();
      const oscillator = ctx.createOscillator();
      const gain = ctx.createGain();
      oscillator.type = 'square';
      oscillator.frequency.value = 460;
      gain.gain.value = 0.025;
      oscillator.connect(gain);
      gain.connect(ctx.destination);
      oscillator.start();
      oscillator.stop(ctx.currentTime + 0.045);
    };

    const applyState = (payload) => {
      Object.assign(state, payload);
      ensureSelectedPet();
    };

    const loadState = async () => {
      const res = await fetch('/api/game');
      applyState(await res.json());
    };

    const post = async (url, actionLabel = '操作', options = {}) => {
      if (busyAction.value) return;
      busyAction.value = actionLabel;
      playClick();
      try {
        const res = await fetch(url, { method: 'POST' });
        applyState(await res.json());
        if (!options.silent) {
          showToast(state.statusMessage || `${actionLabel}完成。`);
        }
      } catch (error) {
        if (!options.silent) {
          showToast(`${actionLabel}失败，请稍后再试。`);
        }
      } finally {
        busyAction.value = '';
      }
    };

    const ensureSelectedPet = () => {
      if (!state.pets.length) {
        selectedPetId.value = null;
        return;
      }
      if (!state.pets.some(pet => pet.id === selectedPetId.value)) {
        selectedPetId.value = state.pets[0].id;
      }
    };

    const selectedPet = computed(() => state.pets.find(pet => pet.id === selectedPetId.value) || state.pets[0] || null);
    const currentPetId = computed(() => selectedPet.value ? selectedPet.value.id : null);

    const feedPet = () => currentPetId.value && post(`/api/game/pets/${currentPetId.value}/feed`, '喂食');
    const cleanPet = () => currentPetId.value && post(`/api/game/pets/${currentPetId.value}/wash`, '清洁');
    const interactPet = () => currentPetId.value && post(`/api/game/pets/${currentPetId.value}/interact`, '互动');
    const playPet = () => interactPet();
    const trainPet = () => currentPetId.value && post(`/api/game/pets/${currentPetId.value}/train`, '训练');
    const restPet = () => currentPetId.value && post(`/api/game/pets/${currentPetId.value}/rest`, '休息');
    const explorePet = () => currentPetId.value && post(`/api/game/pets/${currentPetId.value}/explore`, '探索');
    const adoptPet = () => post('/api/game/adopt', '领养');
    const buyItem = (type) => post(`/api/game/shop/${type}/buy`, '购买');
    const saveGame = () => post('/api/game/save', '保存');
    const silentSaveGame = () => post('/api/game/autosave', '自动保存', { silent: true });
    const upgradeHome = () => post('/api/game/home/upgrade', '升级家园');
    const visitFriend = (name) => post(`/api/game/friends/${encodeURIComponent(name)}/visit`, '拜访好友');
    const claimAchievement = (item) => item.claimable && !item.claimed && post(`/api/game/achievements/${item.key}/claim`, '领取成就');
    const organizeInventory = () => post('/api/game/inventory/organize', '整理背包');
    const renamePet = () => {
      if (!currentPetId.value) return;
      const nextName = window.prompt('输入新的宠物名字', petName.value);
      if (nextName === null) return;
      const cleanName = nextName.trim();
      if (!cleanName) {
        showToast('宠物名字不能为空。');
        return;
      }
      post(`/api/game/pets/${currentPetId.value}/rename/${encodeURIComponent(cleanName)}`, '改名');
    };
    const resetGame = () => {
      if (!resetArmed.value) {
        resetArmed.value = true;
        showToast('再次点击“确认重开”会清空当前存档。');
        window.setTimeout(() => {
          resetArmed.value = false;
        }, 3200);
        return;
      }
      resetArmed.value = false;
      post('/api/game/reset', '重开');
    };

    const clampPercent = (value) => Math.max(0, Math.min(100, Math.round(value || 0)));
    const playerName = computed(() => state.playerName || '训练师小艾');
    const petName = computed(() => selectedPet.value?.name || '泡泡');
    const petSpecies = computed(() => selectedPet.value?.species || '蓝柴犬');
    const petForm = computed(() => selectedPet.value?.form || '幼体');
    const foodValue = computed(() => selectedPet.value ? clampPercent(100 - selectedPet.value.hunger) : 0);
    const cleanValue = computed(() => selectedPet.value ? clampPercent(selectedPet.value.health) : 0);
    const moodValue = computed(() => selectedPet.value ? clampPercent(selectedPet.value.mood) : 0);
    const loveValue = computed(() => selectedPet.value ? clampPercent(selectedPet.value.affection) : 0);
    const energyValue = computed(() => selectedPet.value ? clampPercent((selectedPet.value.health + selectedPet.value.mood + 100 - selectedPet.value.hunger) / 3) : 0);
    const energyLabel = computed(() => energyValue.value >= 75 ? '活力充足' : energyValue.value >= 45 ? '需要照护' : '状态偏低');
    const moodLabel = computed(() => moodValue.value >= 75 ? '心情愉快' : moodValue.value >= 45 ? '情绪平稳' : '需要互动');
    const petStatusCards = computed(() => [
      { label: '饱食度', value: foodValue.value, icon: icon('status_food.png'), barClass: 'bar-food' },
      { label: '清洁度', value: cleanValue.value, icon: icon('status_clean_water.png'), barClass: 'bar-clean' },
      { label: '活力值', value: energyValue.value, icon: icon('status_energy.png'), barClass: 'bar-energy' },
      { label: '亲密度', value: loveValue.value, icon: icon('status_love.png'), barClass: 'bar-love' }
    ]);
    const expRate = computed(() => Math.min(100, Math.max(28, state.day * 6)));
    const homeLevel = computed(() => Math.max(1, state.homeLevel || 3));
    const regionScore = computed(() => 320 + homeLevel.value * 100 + state.pets.length * 35);
    const regionTarget = computed(() => 600 + homeLevel.value * 120);
    const regionRate = computed(() => Math.min(100, Math.round((regionScore.value / regionTarget.value) * 100)));
    const upgradeCost = computed(() => 120 + (homeLevel.value + 1) * 45);
    const petGrowthValue = computed(() => {
      const pet = selectedPet.value;
      if (!pet) return 0;
      if (pet.growth > 0) return Math.min(500, pet.growth);
      const total = 100 - pet.hunger + pet.mood + pet.health + pet.affection;
      return Math.min(500, Math.round(total * 1.25));
    });
    const petGrowth = computed(() => `${petGrowthValue.value}/500`);
    const growthRate = computed(() => petGrowthValue.value / 5);
    const formProgress = computed(() => {
      const pet = selectedPet.value;
      if (!pet) return 0;
      const growthPart = Math.min(1, petGrowthValue.value / 360);
      const affectionPart = Math.min(1, (pet.affection || 0) / 70);
      return Math.round(Math.min(growthPart, affectionPart) * 100);
    });
    const formHint = computed(() => petForm.value === '福瑞' ? '福瑞形态' : `福瑞进度 ${formProgress.value}%`);
    const petCards = computed(() => state.pets);
    const trainingCourses = [
      { title: '活力训练', desc: '提升活力和恢复速度。', cost: '40 金币', icon: icon('menu_train.png') },
      { title: '灵巧挑战', desc: '提高宠物互动效率。', cost: '30 金币', icon: icon('action_train_whistle.png') },
      { title: '亲密陪练', desc: '增强亲密度和心情。', cost: '20 金币', icon: icon('action_interact_hand.png') }
    ];
    const exploreMissions = [
      { title: '松林采集', desc: '获取草料和材料。', reward: '材料 +12', icon: icon('menu_explore.png') },
      { title: '湖边巡游', desc: '带回玩具与金币。', reward: '金币 +60', icon: icon('menu_explore.png') },
      { title: '夜间寻宝', desc: '有机会获得稀有道具。', reward: '稀有掉落', icon: icon('menu_explore.png') }
    ];
    const taskBoard = computed(() => [
      { label: '喂食 2 次', reward: 'EXP 40', done: state.history.filter(line => line.includes('喂食')).length >= 2 },
      { label: '互动 1 次', reward: 'EXP 30', done: state.history.some(line => line.includes('互动')) },
      { label: '清洁 1 次', reward: 'EXP 30', done: state.history.some(line => line.includes('洗护')) },
      { label: '训练 1 次', reward: 'EXP 30', done: state.history.some(line => line.includes('训练')) }
    ]);
    const shopItems = [
      { name: '营养粮', type: 'food', desc: '恢复饱食度。', price: 25, icon: icon('inv_grain.png') },
      { name: '玩具球', type: 'toy', desc: '提升互动效率。', price: 40, icon: icon('inv_ball.png') },
      { name: '洗护液', type: 'care', desc: '恢复清洁状态。', price: 30, icon: icon('inv_tube_blue.png') },
      { name: '木屋券', type: 'place', desc: '扩充家园。', price: 120, icon: icon('inv_house.png') },
      { name: '牧草', type: 'material', desc: '基础家园材料。', price: 18, icon: icon('inv_grass.png') }
    ];
    const discoveredSpecies = computed(() => new Set(state.pets.map(pet => pet.species)));
    const discoveredFurrySpecies = computed(() => new Set(state.pets.filter(pet => pet.form === '福瑞').map(pet => pet.species)));
    const hasFurryDog = computed(() => Array.from(discoveredFurrySpecies.value).some(species => species.includes('犬') || species.includes('柯基') || species.includes('狗')));
    const hasFurryRabbit = computed(() => Array.from(discoveredFurrySpecies.value).some(species => species.includes('兔')));
    const albumItems = computed(() => [
      { name: '蓝柴幼体', desc: '活泼忠诚的狗类幼体。', state: discoveredSpecies.value.has('蓝柴犬') ? '已收录' : '待领养', icon: pic('狗1.png') },
      { name: '绿柯基幼体', desc: '行动敏捷的狗类幼体。', state: discoveredSpecies.value.has('绿柯基') ? '已收录' : '待领养', icon: pic('狗2.png') },
      { name: '粉绒兔幼体', desc: '亲和力高的兔类幼体。', state: discoveredSpecies.value.has('粉绒兔') ? '已收录' : '待领养', icon: pic('兔1.png') },
      { name: '棕耳兔幼体', desc: '擅长探索的兔类幼体。', state: discoveredSpecies.value.has('棕耳兔') ? '已收录' : '待领养', icon: pic('兔2.png') },
      { name: '福瑞犬形态', desc: '狗类宠物成长后的福瑞形态。', state: hasFurryDog.value ? '已收录' : '待成长', icon: pic('兽人狗1.png') },
      { name: '福瑞兔形态', desc: '兔类宠物成长后的福瑞形态。', state: hasFurryRabbit.value ? '已收录' : '待成长', icon: pic('兽人兔1.png') },
      { name: '森林小屋', desc: '当前主居所。', state: '已收录', icon: pic('家园.png') },
      { name: '跑轮', desc: '玩具收藏。', state: '已收录', icon: icon('inv_wheel.png') },
      { name: '水瓶', desc: '场景道具。', state: '已收录', icon: icon('scene_water_bottle.png') }
    ]);
    const lockedAlbumCount = computed(() => albumItems.value.filter(item => item.state !== '已收录').length);
    const achievementItems = computed(() => [
      {
        key: 'adopt',
        title: '首次领养',
        desc: '宠物数量达到 5 只。',
        progress: Math.min(100, Math.round((state.pets.length / 5) * 100)),
        reward: 80,
        claimed: (state.claimedAchievements & 1) === 1,
        icon: icon('menu_achievement.png')
      },
      {
        key: 'care',
        title: '连续照护',
        desc: '完成 3 次喂食、互动或清洁。',
        progress: Math.min(100, Math.round((state.history.filter(line => line.includes('喂食') || line.includes('互动') || line.includes('洗护')).length / 3) * 100)),
        reward: 120,
        claimed: (state.claimedAchievements & 2) === 2,
        icon: icon('menu_achievement.png')
      },
      {
        key: 'home',
        title: '家园扩建',
        desc: '森林小屋升级到 Lv.4。',
        progress: Math.min(100, Math.round((homeLevel.value / 4) * 100)),
        reward: 160,
        claimed: (state.claimedAchievements & 4) === 4,
        icon: icon('menu_achievement.png')
      },
      {
        key: 'furry',
        title: '福瑞觉醒',
        desc: '任意宠物成长为福瑞形态。',
        progress: state.pets.some(pet => pet.form === '福瑞') ? 100 : Math.max(0, ...state.pets.map(pet => {
          const growthPart = Math.min(1, (pet.growth || 0) / 360);
          const affectionPart = Math.min(1, (pet.affection || 0) / 70);
          return Math.round(Math.min(growthPart, affectionPart) * 100);
        })),
        reward: 240,
        claimed: (state.claimedAchievements & 8) === 8,
        icon: icon('menu_achievement.png')
      }
    ].map(item => ({ ...item, claimable: item.progress >= 100 && !item.claimed })));
    const claimableAchievements = computed(() => achievementItems.value.filter(item => item.claimable).length);
    const friendItems = [
      { name: '小北', home: '花园小屋', status: '在线', avatar: pic('trainer_01_forest.png') },
      { name: '阿橙', home: '湖畔营地', status: '刚刚访问', avatar: pic('trainer_02_beach.png') },
      { name: '米夏', home: '山丘牧场', status: '可拜访', avatar: pic('trainer_06_snow.png') }
    ];
    const settingsRows = computed(() => [
      { key: 'sound', label: '音效', desc: '控制按钮和提示音。', value: settings.sound ? '开启' : '关闭', active: settings.sound },
      { key: 'animation', label: '动画', desc: '控制过渡与动作反馈。', value: settings.animation ? '流畅' : '关闭', active: settings.animation },
      { key: 'autosave', label: '自动保存', desc: '每 60 秒写入当前存档。', value: settings.autosave ? '开启' : '关闭', active: settings.autosave },
      { key: 'compact', label: '紧凑界面', desc: '减少面板间距，适合小屏操作。', value: settings.compact ? '开启' : '关闭', active: settings.compact }
    ]);

    const planItems = taskBoard;
    const completedPlans = computed(() => taskBoard.value.filter(item => item.done).length);

    const inventoryIconByType = {
      food: icon('inv_grain.png'),
      toy: icon('inv_ball.png'),
      care: icon('inv_tube_blue.png'),
      place: icon('inv_house.png'),
      material: icon('inv_grass.png')
    };

    const decorativeInventory = [
      { name: '葵花籽', type: 'food', quantity: 28, icon: icon('inv_flower_ring.png') },
      { name: '水果干', type: 'food', quantity: 15, icon: icon('inv_fruit.png') },
      { name: '牧草', type: 'material', quantity: 22, icon: icon('inv_grass.png') },
      { name: '磨牙棒', type: 'care', quantity: 7, icon: icon('inv_toothbrush.png') },
      { name: '跑轮', type: 'toy', quantity: 1, icon: icon('inv_wheel.png') },
      { name: '浴沙', type: 'care', quantity: 4, icon: icon('inv_bath_sand.png') }
    ];

    const fullInventory = computed(() => {
      const realItems = state.inventory.map(item => ({
        ...item,
        icon: inventoryIconByType[item.type] || icon('inv_grass.png')
      }));
      const byName = new Set(realItems.map(item => item.name));
      const extras = decorativeInventory.filter(item => !byName.has(item.name));
      return [...realItems, ...extras].slice(0, 10);
    });

    const visibleInventory = computed(() => {
      if (activeInventoryTab.value === '全部') return fullInventory.value;
      const typeByTab = {
        '食物': 'food',
        '玩具': 'toy',
        '道具': 'care',
        '材料': 'material'
      };
      return fullInventory.value.filter(item => item.type === typeByTab[activeInventoryTab.value]);
    });

    const bagUsed = computed(() => state.inventory.reduce((sum, item) => sum + item.quantity, 0));
    const bagLimit = computed(() => 40 + homeLevel.value * 5);
    const todayIncome = computed(() => Math.max(0, state.pets.length * 10 + state.day * 5));
    const todayExpense = computed(() => Math.max(0, Math.round(state.totalExpense / Math.max(1, state.day))));
    const weeklyProfit = computed(() => Math.max(0, state.totalIncome - state.totalExpense + todayIncome.value * 3));
    const chartHeights = computed(() => {
      const base = Math.max(15, Math.min(90, Math.round(weeklyProfit.value / 140)));
      return [28, 42, 56, 52, base, 50, Math.min(92, base + 10)];
    });

    const avatarStyle = computed(() => ({ backgroundImage: `url('${pic('trainer_01_forest.png')}')` }));
    const shellClasses = computed(() => ({
      'reduced-motion': !settings.animation,
      'compact-ui': settings.compact,
      'is-busy': Boolean(busyAction.value)
    }));
    const toastMessage = computed(() => toast.value);
    const activePage = computed(() => {
      return pageMap[activeMenu.value] || pageMap['宠物'];
    });
    const resolveValue = (value) => typeof value === 'function' ? value() : value;
    const activePageComputed = computed(() => ({
      title: activePage.value.title,
      subtitle: activePage.value.subtitle,
      icon: activePage.value.icon,
      stats: activePage.value.stats.map(item => ({ ...item, value: resolveValue(item.value) })),
      sideTitle: activePage.value.sideTitle,
      sideItems: activePage.value.sideItems.map(item => ({ ...item, value: resolveValue(item.value) }))
    }));
    const summaryItems = computed(() => activePageComputed.value.sideItems);

    const formatNumber = (value) => new Intl.NumberFormat('zh-CN').format(value || 0);

    const selectMenu = (item) => {
      activeMenu.value = item;
      playClick();
    };

    const selectPet = (id) => {
      selectedPetId.value = id;
      showToast('已切换主宠。');
    };

    const switchPet = () => {
      if (!state.pets.length) return;
      const currentIndex = Math.max(0, state.pets.findIndex(pet => pet.id === currentPetId.value));
      const nextPet = state.pets[(currentIndex + 1) % state.pets.length];
      selectedPetId.value = nextPet.id;
      showToast(`已切换到 ${nextPet.name}。`);
    };

    const petImage = (pet) => {
      const species = pet?.species || '';
      const furry = pet?.form === '福瑞';
      if (species.includes('兔')) return furry ? pic('兽人兔1.png') : pic(species.includes('棕') ? '兔2.png' : '兔1.png');
      if (species.includes('柯基')) return furry ? pic('兽人狗2.png') : pic('狗2.png');
      if (species.includes('犬') || species.includes('狗')) return furry ? pic('兽人狗1.png') : pic('狗1.png');
      if (species.includes('猫') || species.includes('仓鼠') || species.includes('鼠')) return furry ? pic('兽人狗1.png') : pic('狗1.png');
      return furry ? pic('兽人狗1.png') : pic('狗1.png');
    };
    const petSceneImage = computed(() => petImage(selectedPet.value));

    const feedById = (id) => post(`/api/game/pets/${id}/feed`, '喂食');
    const cleanById = (id) => post(`/api/game/pets/${id}/wash`, '清洁');
    const interactById = (id) => post(`/api/game/pets/${id}/interact`, '互动');
    const sellById = (id) => post(`/api/game/pets/${id}/sell`, '卖出');
    const runPlan = (item) => {
      if (item.done) return;
      if (item.label.includes('喂食')) {
        feedPet();
      } else if (item.label.includes('互动') || item.label.includes('玩耍')) {
        interactPet();
      } else if (item.label.includes('清洁')) {
        cleanPet();
      } else {
        trainPet();
      }
    };

    const toggleSetting = (key) => {
      settings[key] = !settings[key];
      saveSettings();
      showToast(`${settingsRows.value.find(item => item.key === key).label}已${settings[key] ? '开启' : '关闭'}。`);
    };

    const showFinanceDetails = () => {
      const net = state.totalIncome - state.totalExpense;
      showToast(`累计收入 ${formatNumber(state.totalIncome)}，累计支出 ${formatNumber(state.totalExpense)}，净收益 ${formatNumber(net)}。`);
    };

    const refreshTime = () => {
      const parts = new Intl.DateTimeFormat('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
      }).formatToParts(new Date());
      const get = (type) => parts.find(part => part.type === type)?.value || '';
      currentTime.value = `${get('month')}/${get('day')} ${get('hour')}:${get('minute')}`;
    };

    onMounted(() => {
      loadSettings();
      refreshTime();
      loadState();
      window.setInterval(refreshTime, 30000);
      window.setInterval(() => {
        if (settings.autosave && !busyAction.value) {
          silentSaveGame();
        }
      }, 60000);
    });

    return {
      state,
      menus,
      inventoryTabs,
      activeMenu,
      activeInventoryTab,
      currentTime,
      shellClasses,
      busyAction,
      toastMessage,
      resetArmed,
      activePage: activePageComputed,
      selectedPet,
      playerName,
      petName,
      petSpecies,
      petForm,
      formHint,
      foodValue,
      cleanValue,
      moodValue,
      loveValue,
      energyValue,
      energyLabel,
      moodLabel,
      petStatusCards,
      expRate,
      regionRate,
      regionScore,
      regionTarget,
      homeLevel,
      upgradeCost,
      petGrowth,
      petGrowthValue,
      growthRate,
      formProgress,
      petSceneImage,
      completedPlans,
      visibleInventory,
      bagUsed,
      bagLimit,
      todayIncome,
      todayExpense,
      weeklyProfit,
      chartHeights,
      avatarStyle,
      petCards,
      trainingCourses,
      exploreMissions,
      planItems,
      taskBoard,
      shopItems,
      albumItems,
      achievementItems,
      friendItems,
      settingsRows,
      gameRules,
      summaryItems,
      formatNumber,
      selectMenu,
      selectPet,
      switchPet,
      renamePet,
      petImage,
      feedById,
      cleanById,
      interactById,
      sellById,
      runPlan,
      toggleSetting,
      showFinanceDetails,
      feedPet,
      cleanPet,
      interactPet,
      playPet,
      trainPet,
      restPet,
      explorePet,
      adoptPet,
      buyItem,
      saveGame,
      resetGame,
      upgradeHome,
      visitFriend,
      claimAchievement,
      organizeInventory
    };
  }
}).mount('#app');
