/* ========================================
   Tag Game - 追逐游戏（简化版）
   ======================================== */

const CONFIG = {
    playerCount: 4,
    gameTime: 120,
    playerSize: 25,
    playerSpeed: 5,
    jumpSpeed: 10,
    gravity: 0.8,
    // 跳跃手感优化
    coyoteTime: 6,      // 土狼时间（帧数），离开平台后仍可跳跃
    jumpBuffer: 6,      // 跳跃缓冲（帧数），落地前按跳跃仍能触发
    selectedMap: 1,
    colors: ['#00ffff', '#ff00ff', '#ffff00', '#00ff00'],
    tagCooldown: 500 // 标记冷却时间（毫秒）
};

const CONTROLS = [
    { up: 'w', down: 's', left: 'a', right: 'd', jump: 'w', zap: 'e' },
    { up: 'ArrowUp', down: 'ArrowDown', left: 'ArrowLeft', right: 'ArrowRight', jump: 'ArrowUp', zap: 'l' },
    { up: 'i', down: 'k', left: 'j', right: 'l', jump: 'i', zap: 'u' },
    { up: 't', down: 'g', left: 'f', right: 'h', jump: 't', zap: 'o' }
];

const MAP_BACKGROUNDS = {
    1: 'assets/city_background.png',
    2: 'assets/volcano_bar_1.png',
    3: 'assets/forest_background.jpg'
};

const AUDIO_CONFIG = {
    bgmCandidates: ['assets/bgm.mp3', 'assets/bgm.ogg', 'assets/bgm.wav', 'assets/bgm.m4a'],
    homelanderCandidates: ['assets/homelander_theme.mp3'],
    helicopterCandidates: ['assets/helicopter_theme.mp3'],
    helicopterExplosionCandidates: ['assets/helicopter_explosion.mp3'],
    volcanoEruptionCandidates: ['assets/volcano_eruption.mp3'],
    bgmVolume: 0.35,
    homelanderVolume: 0.58,
    helicopterVolume: 0.62,
    helicopterExplosionVolume: 0.85,
    volcanoEruptionVolume: 0.72
};

let gameState = {
    players: [],
    platforms: [],
    timeLeft: 120,
    timerID: null,
    running: false,
    worldWidth: 800,
    worldHeight: 600,
    cameraY: 0,  // 相机偏移量（纵向）
    items: [],   // 场景道具
    itemSpawnTimer: 0,
    lastTime: 0,  // 上次帧时间（用于计算delta time）
    timeStopActive: false,  // 全局时间凝滞
    springs: [],  // 弹簧数组
    traps: [],  // 补鼠夹数组
    trapSpawnTimer: 0,  // 补鼠夹生成计时器
    zapEffects: [],  // 闪电效果数组
    powerOrbs: [],  // 专属能量球数组
    powerOrbSpawnTimer: 0,  // 能量球生成计时器
    currentLevel: 1,
    backgroundPath: 'assets/background.png',
    backgroundImage: null,
    backgroundLoaded: false,
    volcanoEvent: null,
    cityAttackEvent: null
};

let audioState = {
    bgm: null,
    homelander: null,
    helicopter: null,
    helicopterExplosion: null,
    volcanoEruption: null,
    candidateIndex: 0,
    homelanderCandidateIndex: 0,
    helicopterCandidateIndex: 0,
    helicopterExplosionCandidateIndex: 0,
    volcanoEruptionCandidateIndex: 0,
    enabled: true,
    available: false,
    homelanderAvailable: false,
    helicopterAvailable: false,
    helicopterExplosionAvailable: false,
    volcanoEruptionAvailable: false,
    missing: false,
    homelanderMissing: false,
    helicopterMissing: false,
    helicopterExplosionMissing: false,
    volcanoEruptionMissing: false,
    specialActive: false,
    specialTrack: null
};

// 专属能量球配置
const POWER_ORB_CONFIG = {
    spawnInterval: 30,  // 每30秒生成一个
    duration: 15000,    // 15秒后自动消失
    collectRadius: 30,  // 拾取半径
    speedBoostAmount: 2, // 速度加成倍数
    jumpBoostAmount: 1.3, // 跳跃加成倍数
    effectDuration: 10000, // 效果持续10秒
    orbSize: 25
};

// 道具配置
const ITEMS = {
    // 逃生类（普通人专属）
    speedBoots: { name: '加速', type: 'escape', duration: 5000, cooldown: 12000, color: '#ffeb3b', icon: '速' },
    teleportScroll: { name: '瞬移', type: 'escape', duration: 0, cooldown: 25000, color: '#9c27b0', icon: '闪' },

    // 追击类（鬼专属）
    trackerScope: { name: '透视', type: 'chase', duration: 6000, cooldown: 15000, color: '#e91e63', icon: '眼' },
    speedSlow: { name: '减速', type: 'chase', duration: 2000, cooldown: 20000, color: '#ff5722', icon: '锁' },
    catchBoost: { name: '冲刺', type: 'chase', duration: 4000, cooldown: 16000, color: '#f44336', icon: '冲' },

    // 通用干扰
    timeBonus: { name: '延时', type: 'universal', duration: 0, cooldown: 0, color: '#00bcd4', icon: '+10' },

    // 防御保命
    shield: { name: '护盾', type: 'defense', duration: 3000, cooldown: 22000, color: '#2196f3', icon: '盾' },

    // 闪电技能
    zapGun: { name: '电击', type: 'zap', duration: 0, cooldown: 25000, color: '#ffd700', icon: '电' }
};

// 弹簧配置
const SPRING_POWER = 14;  // 弹簧跳跃力（减半）
const SPRING_BOOST_HEIGHT = 300;  // 约4层高度

// 弹簧类
class Spring {
    constructor(x, y, width, height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.bobOffset = Math.random() * Math.PI * 2;
    }

    draw(ctx, offsetY) {
        const bobY = Math.sin(Date.now() / 200 + this.bobOffset) * 3;

        // 弹簧底座
        ctx.fillStyle = '#ff9800';
        ctx.fillRect(this.x, this.y - offsetY + bobY, this.width, this.height);

        // 弹簧圈
        ctx.strokeStyle = '#ffc107';
        ctx.lineWidth = 3;
        ctx.beginPath();
        const segments = 4;
        const segWidth = this.width / segments;
        for (let i = 0; i < segments; i++) {
            const x1 = this.x + i * segWidth;
            const x2 = this.x + (i + 0.5) * segWidth;
            const y1 = this.y - offsetY + bobY - 5;
            const y2 = this.y - offsetY + bobY - 10;
            ctx.moveTo(x1, y1);
            ctx.lineTo(x2, y2);
        }
        ctx.stroke();

        // 弹簧顶部
        ctx.fillStyle = '#ff5722';
        ctx.fillRect(this.x + 2, this.y - offsetY + bobY - 12, this.width - 4, 6);
    }

    checkCollision(p) {
        return p.x + p.size > this.x &&
               p.x - p.size < this.x + this.width &&
               p.y + p.size >= this.y - 5 &&
               p.y + p.size <= this.y + this.height + 10 &&
               p.vy >= 0;
    }
}

// 补鼠夹配置
const TRAP_COUNT = 3;  // 少量补鼠夹
const TRAP_STUN_DURATION = 2000;  // 眩晕时间2秒

const VOLCANO_EVENT_CONFIG = {
    firstDelay: 0,
    warningDuration: 20000,
    eruptionDuration: 10000,
    cooldown: 26000,
    rockSpawnInterval: 520,
    rockStunDuration: 3000,
    rockLifetime: 5000,
    minRockSize: 18,
    maxRockSize: 34,
    shakeStrength: 5,
    meltingPlatformCount: 3,
    platformMeltDuration: 5200
};

const CITY_ATTACK_CONFIG = {
    firstDelay: 12000,
    homelanderDuration: 10000,
    helicopterDuration: 3500,
    explosionDuration: 1600,
    laserDuration: 480,
    laserStunDuration: 3000,
    laserIntervalMin: 850,
    laserIntervalMax: 1450
};

const CITY_ATTACK_ASSETS = {
    homelander: [
        'assets/homelander.png?v=20260611-cutout',
        'assets/homelander.png',
        'assets/homelander.jpg',
        'assets/homelander.webp',
        'assets/祖国人.png',
        'assets/祖国人.jpg',
        'assets/祖国人.webp'
    ],
    kobeHelicopter: [
        'assets/kobe_helicopter.png?v=20260611-cutout',
        'assets/kobe_helicopter.png',
        'assets/kobe_helicopter.jpg',
        'assets/kobe_helicopter.webp',
        'assets/kobe.png',
        'assets/kobe.jpg',
        'assets/kobe.webp',
        'assets/科比直升机.png',
        'assets/科比直升机.jpg',
        'assets/科比直升机.webp'
    ]
};

const CITY_ATTACK_IMAGES = {
    homelander: { image: null, loaded: false },
    kobeHelicopter: { image: null, loaded: false }
};

const VOLCANO_BAR_CONFIG = {
    images: {},
    loaded: {},
    dimensions: {},
    files: {
        stoneTop: 'volcano_stone_top.png',
        lavaPipe: 'volcano_lava_pipe.png',
        stoneMid: 'volcano_stone_mid.png',
        pipeSupported: 'volcano_pipe_supported.png',
        pipeLeft: 'volcano_pipe_left.png',
        pipeRight: 'volcano_pipe_right.png',
        cityStoneTop: 'city_stone_top.png',
        cityBluePipe: 'city_blue_pipe.png',
        cityWarningBar: 'city_warning_bar.png',
        cityPipeSupported: 'city_pipe_supported.png',
        cityCenterBar: 'city_center_bar.png',
        cityPipeLeft: 'city_pipe_left.png',
        cityPipeRight: 'city_pipe_right.png',
        forestLog01: 'taggame_P03_S01.png',
        forestLog02: 'taggame_P03_S02.png',
        forestLog03: 'taggame_P03_S03.png',
        forestLog04: 'taggame_P03_S04.png',
        forestLog05L: 'taggame_P03_S05_L.png',
        forestLog05M: 'taggame_P03_S05_M.png',
        forestLog05R: 'taggame_P03_S05_R.png'
    }
};

// 补鼠夹类
class Trap {
    constructor(x, y) {
        this.x = x;
        this.y = y;
        this.width = 30;
        this.height = 10;
        this.active = true;
        this.bobOffset = Math.random() * Math.PI * 2;
    }

    draw(ctx, offsetY) {
        if (!this.active) return;

        const bobY = Math.sin(Date.now() / 300 + this.bobOffset) * 2;

        // 补鼠夹底座
        ctx.fillStyle = '#8b4513';
        ctx.fillRect(this.x, this.y - offsetY + bobY, this.width, this.height);

        // 铁丝圈
        ctx.strokeStyle = '#cd853f';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.arc(this.x + this.width/2, this.y - offsetY + bobY - 3, 8, Math.PI, 0);
        ctx.stroke();

        // 触发点（红色闪烁）
        const flash = Math.sin(Date.now() / 150) > 0;
        if (flash) {
            ctx.fillStyle = '#ff0000';
            ctx.beginPath();
            ctx.arc(this.x + this.width/2, this.y - offsetY + bobY - 8, 3, 0, Math.PI * 2);
            ctx.fill();
        }
    }

    checkCollision(p) {
        if (!this.active) return false;
        // 玩家中心点经过时触发（不论跳跃还是行走）
        return p.x + p.size > this.x &&
               p.x - p.size < this.x + this.width &&
               p.y > this.y - 20 &&
               p.y < this.y + this.height + 10;
    }
}

function isPlatformSolid(platform) {
    return platform && platform.visible !== false && !platform.broken;
}

function getSpawnablePlatforms() {
    const solid = gameState.platforms.filter(platform => isPlatformSolid(platform));
    return solid.length > 0 ? solid : gameState.platforms;
}

class VolcanoRock {
    constructor(x, y, size) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.age = 0;
        this.lifetime = VOLCANO_EVENT_CONFIG.rockLifetime;
        this.vx = (Math.random() - 0.5) * 3;
        this.vy = 2 + Math.random() * 3;
        this.active = true;
        this.state = 'falling';
        this.rotation = Math.random() * Math.PI * 2;
        this.rollDirection = Math.random() > 0.5 ? 1 : -1;
        this.hitPlayers = new Set();
        this.cracks = Array.from({ length: 5 }, () => ({
            angle: Math.random() * Math.PI * 2,
            length: 0.35 + Math.random() * 0.55,
            bend: (Math.random() - 0.5) * 0.9
        }));
    }

    update(dt) {
        const step = (dt || 16.67) / 16.67;
        this.age += dt || 16.67;

        if (this.state === 'falling') {
            this.vy = Math.min(this.vy + 0.48 * step, 16);
            this.x += this.vx * step;
            this.y += this.vy * step;
            this.rotation += this.vx * 0.03 * step;

            const support = this.getLandingPlatform();
            if (support) {
                this.y = support.y - this.size;
                this.vy = 0;
                this.vx = this.rollDirection * (2.5 + Math.random() * 2.2);
                this.state = 'rolling';
            }
        } else {
            this.x += this.vx * step;
            this.rotation += this.vx * 0.08 * step;

            const support = this.getSupportPlatform();
            if (support) {
                this.y = support.y - this.size;
            } else {
                this.state = 'falling';
                this.vy = 1;
            }
        }

        if (this.x < this.size) {
            this.x = this.size;
            this.vx = Math.abs(this.vx);
        }
        if (this.x > gameState.worldWidth - this.size) {
            this.x = gameState.worldWidth - this.size;
            this.vx = -Math.abs(this.vx);
        }

        if (this.age >= this.lifetime || this.y > gameState.worldHeight + this.size * 3) {
            this.active = false;
        }
    }

    getLandingPlatform() {
        return gameState.platforms.find(plat =>
            isPlatformSolid(plat) &&
            this.vy >= 0 &&
            this.x + this.size > plat.x &&
            this.x - this.size < plat.x + plat.width &&
            this.y + this.size >= plat.y &&
            this.y + this.size <= plat.y + Math.max(28, plat.height + this.vy + 8)
        );
    }

    getSupportPlatform() {
        return gameState.platforms.find(plat =>
            isPlatformSolid(plat) &&
            this.x + this.size * 0.55 > plat.x &&
            this.x - this.size * 0.55 < plat.x + plat.width &&
            Math.abs((this.y + this.size) - plat.y) < 18
        );
    }

    checkPlayerCollision(player) {
        if (!this.active || this.hitPlayers.has(player.id)) return false;
        const dx = player.x - this.x;
        const dy = player.y - this.y;
        const distance = Math.sqrt(dx * dx + dy * dy);
        return distance < player.size + this.size * 0.82;
    }

    draw(ctx, offsetY) {
        const drawY = this.y - offsetY;
        const alpha = Math.min(1, Math.max(0, (this.lifetime - this.age) / 900));

        ctx.save();
        ctx.globalAlpha = alpha;
        ctx.translate(this.x, drawY);
        ctx.rotate(this.rotation);

        ctx.shadowColor = '#ff5a12';
        ctx.shadowBlur = 18;
        const gradient = ctx.createRadialGradient(
            -this.size * 0.35, -this.size * 0.45, this.size * 0.1,
            0, 0, this.size
        );
        gradient.addColorStop(0, '#8a8177');
        gradient.addColorStop(0.45, '#4d4642');
        gradient.addColorStop(1, '#1f1c1d');

        ctx.beginPath();
        const points = 16;
        for (let i = 0; i < points; i++) {
            const angle = (i / points) * Math.PI * 2;
            const radius = this.size * (0.82 + Math.sin(i * 2.1) * 0.08 + Math.cos(i * 3.4) * 0.06);
            const x = Math.cos(angle) * radius;
            const y = Math.sin(angle) * radius;
            if (i === 0) ctx.moveTo(x, y);
            else ctx.lineTo(x, y);
        }
        ctx.closePath();
        ctx.fillStyle = gradient;
        ctx.fill();
        ctx.lineWidth = 2;
        ctx.strokeStyle = '#161313';
        ctx.stroke();

        ctx.lineCap = 'round';
        this.cracks.forEach(crack => {
            const start = this.size * 0.15;
            const end = this.size * crack.length;
            const mid = (start + end) / 2;
            const x1 = Math.cos(crack.angle) * start;
            const y1 = Math.sin(crack.angle) * start;
            const x2 = Math.cos(crack.angle + crack.bend) * mid;
            const y2 = Math.sin(crack.angle + crack.bend) * mid;
            const x3 = Math.cos(crack.angle) * end;
            const y3 = Math.sin(crack.angle) * end;

            ctx.shadowColor = '#ff7a18';
            ctx.shadowBlur = 12;
            ctx.strokeStyle = '#ff5a12';
            ctx.lineWidth = Math.max(2, this.size * 0.1);
            ctx.beginPath();
            ctx.moveTo(x1, y1);
            ctx.lineTo(x2, y2);
            ctx.lineTo(x3, y3);
            ctx.stroke();

            ctx.shadowBlur = 0;
            ctx.strokeStyle = '#ffd06a';
            ctx.lineWidth = 1.2;
            ctx.beginPath();
            ctx.moveTo(x1, y1);
            ctx.lineTo(x2, y2);
            ctx.lineTo(x3, y3);
            ctx.stroke();
        });

        ctx.restore();
    }
}

// 闪电效果类
class ZapEffect {
    constructor(startX, endX, y) {
        this.startX = startX;
        this.endX = endX;
        this.y = y;
        this.lifetime = 30; // 闪电显示30帧
        this.points = this.generateLightningPoints();
    }

    generateLightningPoints() {
        const points = [];
        const segments = 8;
        const xStep = (this.endX - this.startX) / segments;
        let currentY = this.y;

        for (let i = 0; i <= segments; i++) {
            points.push({
                x: this.startX + i * xStep,
                y: currentY + (Math.random() - 0.5) * 30
            });
            if (i < segments) {
                currentY += (Math.random() - 0.5) * 40;
            }
        }
        return points;
    }

    draw(ctx, offsetY) {
        if (this.lifetime <= 0) return;

        const alpha = this.lifetime / 30;
        ctx.strokeStyle = `rgba(255, 215, 0, ${alpha})`;
        ctx.lineWidth = 4;
        ctx.shadowColor = '#ffd700';
        ctx.shadowBlur = 20;

        ctx.beginPath();
        ctx.moveTo(this.points[0].x, this.points[0].y - offsetY);
        for (let i = 1; i < this.points.length; i++) {
            ctx.lineTo(this.points[i].x, this.points[i].y - offsetY);
        }
        ctx.stroke();

        // 闪电分支
        ctx.lineWidth = 2;
        ctx.strokeStyle = `rgba(255, 255, 0, ${alpha * 0.7})`;
        for (let i = 1; i < this.points.length - 1; i++) {
            if (Math.random() > 0.5) {
                ctx.beginPath();
                ctx.moveTo(this.points[i].x, this.points[i].y - offsetY);
                ctx.lineTo(
                    this.points[i].x + (Math.random() - 0.5) * 40,
                    this.points[i].y - offsetY + (Math.random() - 0.5) * 40
                );
                ctx.stroke();
            }
        }

        ctx.shadowBlur = 0;
    }

    update() {
        this.lifetime--;
    }

    isExpired() {
        return this.lifetime <= 0;
    }
}

// 专属能量球类
class PowerOrb {
    constructor(targetPlayerId) {
        this.targetPlayerId = targetPlayerId; // 专属目标玩家
        this.orbId = 'orb_' + targetPlayerId;
        this.size = POWER_ORB_CONFIG.orbSize;
        this.active = true;
        this.bobOffset = Math.random() * Math.PI * 2;
        this.rotationAngle = 0;

        // 随机位置（确保在平台上）
        const platforms = getSpawnablePlatforms();
        const plat = platforms[Math.floor(Math.random() * platforms.length)];
        this.x = plat.x + Math.random() * (plat.width - 40) + 20;
        this.y = plat.y - 50;

        // 持续时间
        this.lifetime = POWER_ORB_CONFIG.duration;
    }

    draw(ctx, offsetY) {
        if (!this.active) return;

        const bobY = Math.sin(Date.now() / 200 + this.bobOffset) * 5;
        const drawY = this.y - offsetY + bobY;
        this.rotationAngle += 0.05;

        // 目标玩家颜色
        const targetPlayer = gameState.players[this.targetPlayerId];
        const playerColor = targetPlayer ? targetPlayer.color : '#ffffff';
        const glowColor = this.targetPlayerId === 0 ? '#00ffff' :
                         this.targetPlayerId === 1 ? '#ff00ff' :
                         this.targetPlayerId === 2 ? '#ffff00' : '#00ff00';

        // 外层发光
        ctx.shadowColor = glowColor;
        ctx.shadowBlur = 30;

        // 能量环（旋转）
        ctx.save();
        ctx.translate(this.x, drawY);
        ctx.rotate(this.rotationAngle);

        // 外圈
        ctx.beginPath();
        ctx.arc(0, 0, this.size, 0, Math.PI * 2);
        ctx.strokeStyle = glowColor;
        ctx.lineWidth = 3;
        ctx.stroke();

        // 内部能量球
        const gradient = ctx.createRadialGradient(0, 0, 0, 0, 0, this.size - 5);
        gradient.addColorStop(0, '#ffffff');
        gradient.addColorStop(0.3, playerColor);
        gradient.addColorStop(1, glowColor);
        ctx.fillStyle = gradient;
        ctx.beginPath();
        ctx.arc(0, 0, this.size - 8, 0, Math.PI * 2);
        ctx.fill();

        // 旋转的三角形装饰
        ctx.fillStyle = '#ffffff';
        for (let i = 0; i < 3; i++) {
            ctx.save();
            ctx.rotate(i * Math.PI * 2 / 3);
            ctx.beginPath();
            ctx.moveTo(this.size - 3, 0);
            ctx.lineTo(this.size - 10, -5);
            ctx.lineTo(this.size - 10, 5);
            ctx.closePath();
            ctx.fill();
            ctx.restore();
        }

        ctx.restore();

        // 玩家标识（底部显示属于谁）
        ctx.fillStyle = playerColor;
        ctx.font = 'bold 12px Arial';
        ctx.textAlign = 'center';
        ctx.fillText('P' + (this.targetPlayerId + 1), this.x, drawY + this.size + 15);

        // 专属标记
        ctx.fillStyle = '#ffd700';
        ctx.font = '10px Arial';
        ctx.fillText('★', this.x, drawY - this.size - 5);

        ctx.shadowBlur = 0;
    }

    checkCollision(p) {
        if (!this.active) return false;
        if (p.id !== this.targetPlayerId) return false; // 只有专属玩家能拾取

        const dx = p.x - this.x;
        const dy = p.y - this.y;
        return Math.sqrt(dx * dx + dy * dy) < p.size + this.size + POWER_ORB_CONFIG.collectRadius;
    }

    update(dt) {
        this.lifetime -= dt;
        if (this.lifetime <= 0) {
            this.active = false;
        }
    }

    isExpired() {
        return this.lifetime <= 0;
    }
}

// 道具类
class Item {
    constructor(x, y, type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.config = ITEMS[type];
        this.size = 20;
        this.bobOffset = Math.random() * Math.PI * 2;
        this.active = true;
    }

    draw(ctx, offsetY) {
        if (!this.active) return;

        const bobY = Math.sin(Date.now() / 300 + this.bobOffset) * 3;

        // 发光效果
        ctx.shadowColor = this.config.color;
        ctx.shadowBlur = 15;

        // 圆形背景
        ctx.beginPath();
        ctx.arc(this.x, this.y - offsetY + bobY, this.size, 0, Math.PI * 2);
        ctx.fillStyle = this.config.color;
        ctx.fill();

        // 图标文字
        ctx.shadowBlur = 0;
        ctx.fillStyle = '#fff';
        ctx.font = 'bold 14px Arial';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(this.config.icon, this.x, this.y - offsetY + bobY);
    }

    checkCollision(p) {
        const dx = p.x - this.x;
        const dy = p.y - this.y;
        return Math.sqrt(dx * dx + dy * dy) < p.size + this.size;
    }
}

// 生成道具
function spawnItem() {
    const types = Object.keys(ITEMS);
    const type = types[Math.floor(Math.random() * types.length)];

    // 随机位置（确保在平台上）
    let x, y;
    const platforms = getSpawnablePlatforms();
    const plat = platforms[Math.floor(Math.random() * platforms.length)];
    x = plat.x + Math.random() * (plat.width - 40) + 20;
    y = plat.y - 30;

    const item = new Item(x, y, type);
    gameState.items.push(item);

    // 15秒后自动消失
    setTimeout(() => {
        item.active = false;
        const idx = gameState.items.indexOf(item);
        if (idx > -1) gameState.items.splice(idx, 1);
    }, 15000);
}

// 使用道具（type可选，自动从item获取或直接传type）
function useItem(p, forceType) {
    // 从场景道具中获取类型
    const type = forceType || (p.item ? p.item.type : null);
    if (!type) return;

    const config = ITEMS[type];
    if (!config) return;

    console.log('使用道具:', type); // 调试日志

    // 创建道具使用特效
    createItemEffect(p, type);

    switch (type) {
        // 逃生类
        case 'speedBoots':
            p.speedBoost = true;
            p.speedBoostTimer = config.duration;
            p.baseSpeed = CONFIG.playerSpeed;
            p.speedBoostEffect = CONFIG.playerSpeed * 2; // 2倍速度
            break;

        case 'teleportScroll':
            // 保存旧位置用于特效
            const oldX = p.x;
            const oldY = p.y;
            // 向面朝方向瞬移150像素
            p.x += p.facingRight ? 150 : -150;
            // 边界限制
            p.x = Math.max(p.size, Math.min(gameState.worldWidth - p.size, p.x));
            // 创建传送特效（起点和终点）
            createItemEffect(p, 'teleportStart', oldX, oldY);
            createItemEffect(p, 'teleportEnd', p.x, p.y);
            break;

        // 追击类
        case 'trackerScope':
            p.trackerActive = true;
            p.trackerTimer = config.duration;
            break;

        case 'speedSlow':
            p.speedSlowActive = true;
            p.speedSlowTimer = config.duration;
            break;

        case 'catchBoost':
            p.catchBoostActive = true;
            p.catchBoostTimer = config.duration;
            p.baseSpeed = CONFIG.playerSpeed;
            p.catchBoostEffect = CONFIG.playerSpeed * 2.5; // 2.5倍速度
            break;

        // 通用干扰
        case 'timeBonus':
            gameState.timeLeft += 10; // 增加10秒游戏时间
            updateTimer(); // 更新显示
            break;

        // 防御保命
        case 'shield':
            p.shieldActive = true;
            p.shieldTimer = config.duration;
            break;

        // 闪电技能
        case 'zapGun':
            p.hasZap = true;
            p.zapCooldown = false;
            p.zapCooldownTimer = 0;
            break;
    }

    // 消耗道具
    p.item = null;
    playSound();
}

// 更新道具效果
function updateItemEffects(p, dt) {
    const deltaTime = dt || 16.67;

    // 速度提升
    if (p.speedBoost) {
        p.speedBoostTimer -= deltaTime;
        if (p.speedBoostTimer <= 0) {
            p.speedBoost = false;
        }
    }

    // 追踪视野
    if (p.trackerActive) {
        p.trackerTimer -= deltaTime;
        if (p.trackerTimer <= 0) {
            p.trackerActive = false;
        }
    }

    // 减速陷阱 - 激活时影响附近玩家
    if (p.speedSlowActive) {
        p.speedSlowTimer -= deltaTime;
        // 对范围内的敌人减速
        for (let other of gameState.players) {
            if (other === p || other.isIT === p.isIT) continue; // 同伙不减速
            const dx = other.x - p.x;
            const dy = other.y - p.y;
            const dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < 80) { // 80像素范围
                other.speedSlowed = true;
                other.speedSlowEffect = CONFIG.playerSpeed * 0.5; // 减速50%
            }
        }
        if (p.speedSlowTimer <= 0) {
            p.speedSlowActive = false;
        }
    }

    // 清除其他玩家的减速效果
    for (let other of gameState.players) {
        if (!p.speedSlowActive) {
            other.speedSlowed = false;
            other.speedSlowEffect = 0;
        }
    }

    // 抓人加速
    if (p.catchBoostActive) {
        p.catchBoostTimer -= deltaTime;
        p.catchBoostEffect = true;
        if (p.catchBoostTimer <= 0) {
            p.catchBoostActive = false;
            p.catchBoostEffect = false;
        }
    }

    // 时间凝滞 - 全局效果
    if (p.timeStopActive) {
        p.timeStopTimer -= deltaTime;
        if (p.timeStopTimer <= 0) {
            p.timeStopActive = false;
        }
    }

    // 护盾
    if (p.shieldActive) {
        p.shieldTimer -= deltaTime;
        if (p.shieldTimer <= 0) {
            p.shieldActive = false;
        }
    }

    // 身份互换冷却
    if (p.swapCooldown) {
        p.swapCooldownTimer -= deltaTime;
        if (p.swapCooldownTimer <= 0) {
            p.swapCooldown = false;
        }
    }

    // 定身效果（补鼠夹）
    if (p.zapped) {
        p.zappedTimer -= deltaTime;
        if (p.zappedTimer <= 0) {
            p.zapped = false;
        }
    }

    // 专属能量球效果
    if (p.powerOrbBoost) {
        p.powerOrbTimer -= deltaTime;
        if (p.powerOrbTimer <= 0) {
            p.powerOrbBoost = false;
            p.powerOrbEffect = false;
        }
    }
}

// 绘制道具效果
function drawItemEffects(ctx, p, offsetY) {
    const drawY = p.y - offsetY;

    // 追踪视野（显示所有玩家位置给鬼）
    if (p.trackerActive && p.isIT) {
        for (let other of gameState.players) {
            if (other === p) continue;
            ctx.beginPath();
            ctx.arc(other.x, other.y - offsetY, other.size + 5, 0, Math.PI * 2);
            ctx.strokeStyle = '#e91e63';
            ctx.lineWidth = 3;
            ctx.setLineDash([5, 5]);
            ctx.stroke();
            ctx.setLineDash([]);
        }
    }

    // 减速陷阱范围显示
    if (p.speedSlowActive) {
        ctx.beginPath();
        ctx.arc(p.x, drawY, 80, 0, Math.PI * 2);
        ctx.strokeStyle = 'rgba(255,87,34,0.5)';
        ctx.lineWidth = 2;
        ctx.setLineDash([5, 5]);
        ctx.stroke();
        ctx.setLineDash([]);
    }

    // 时间凝滞效果
    if (p.timeStopActive) {
        ctx.fillStyle = 'rgba(0,188,212,0.3)';
        ctx.font = 'bold 16px Arial';
        ctx.textAlign = 'center';
        ctx.fillText('⏸ 时间凝滞!', p.x, drawY - p.size - 20);
    }

    // 定身效果（补鼠夹）
    if (p.zapped) {
        ctx.fillStyle = '#ff0000';
        ctx.font = 'bold 12px Arial';
        ctx.textAlign = 'center';
        ctx.fillText('!!!', p.x, drawY - p.size - 20);
    }

    // 玩家持有道具图标（头顶显示）
    if (p.item) {
        // 背景框
        ctx.fillStyle = 'rgba(0,0,0,0.5)';
        ctx.fillRect(p.x - 15, drawY - p.size - 35, 30, 20);
        ctx.fillStyle = p.item.config.color;
        ctx.font = 'bold 14px Arial';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(p.item.config.icon, p.x, drawY - p.size - 25);
    }

    // 速度提升指示
    if (p.speedBoost) {
        ctx.fillStyle = '#ffeb3b';
        ctx.font = '10px Arial';
        ctx.textAlign = 'center';
        ctx.fillText('⚡', p.x, drawY + p.size + 12);
    }

    // 抓人加速指示
    if (p.catchBoostActive) {
        ctx.fillStyle = '#f44336';
        ctx.font = '10px Arial';
        ctx.textAlign = 'center';
        ctx.fillText('👻', p.x, drawY + p.size + 12);
    }

    // 专属能量球加成指示
    if (p.powerOrbBoost) {
        ctx.fillStyle = '#ffd700';
        ctx.font = 'bold 12px Arial';
        ctx.textAlign = 'center';
        // 显示剩余时间
        const timeLeft = Math.ceil(p.powerOrbTimer / 1000);
        ctx.fillText('⚡' + timeLeft + 's', p.x, drawY + p.size + 25);
    }
}

const keys = {};

// 平台类
class Platform {
    constructor(x, y, width, height, color, options = {}) {
        this.x = x;
        this.y = y;
        this.baseX = x;
        this.baseY = y;
        this.width = width;
        this.height = height;
        this.color = color;
        this.moveX = options.moveX || 0;
        this.moveY = options.moveY || 0;
        this.speed = options.speed || 0;
        this.phase = options.phase || 0;
        this.rotationSpeed = options.rotationSpeed || 0;
        this.angle = options.angle || 0;
        this.baseAngle = this.angle;
        this.rotationRange = Math.min(options.rotationRange || 0, Math.PI / 3);
        this.volcano = !!options.volcano;
        this.sprite = options.sprite || null;
        this.visualHeight = options.visualHeight || height;
        this.visualOffsetY = options.visualOffsetY || 0;
        this.preserveAspect = !!options.preserveAspect;
        this.compositeSprite = options.compositeSprite || null;
        this.visible = options.visible !== false;
        this.isMelting = false;
        this.meltProgress = 0;
        this.broken = false;
        this.canMelt = options.canMelt !== false;
    }

    update(time) {
        if (!this.moveX && !this.moveY && !this.rotationRange) return;
        const wave = Math.sin(time * this.speed + this.phase);
        this.x = this.baseX + this.moveX * wave;
        this.y = this.baseY + this.moveY * wave;
        if (this.rotationRange) {
            this.angle = this.baseAngle + Math.sin(time * this.rotationSpeed + this.phase) * this.rotationRange;
        }
    }

    draw(ctx, offsetY) {
        if (!this.visible || this.broken) return;

        ctx.save();
        const meltSag = this.isMelting ? this.meltProgress * 10 : 0;
        ctx.translate(this.x + this.width / 2, this.y - offsetY + this.height / 2 + meltSag);
        ctx.rotate(this.angle);

        if (this.compositeSprite) {
            const parts = this.compositeSprite;
            const allLoaded = parts.every(s => VOLCANO_BAR_CONFIG.loaded[s]);
            const allDimsExist = parts.every(s => VOLCANO_BAR_CONFIG.dimensions[s]);
            if (allLoaded && allDimsExist) {
                const drawY = -this.height / 2 + this.visualOffsetY;
                const leftDim = VOLCANO_BAR_CONFIG.dimensions[parts[0]];
                const midDim = VOLCANO_BAR_CONFIG.dimensions[parts[1]];
                const rightDim = VOLCANO_BAR_CONFIG.dimensions[parts[2]];
                const leftW = this.visualHeight * (leftDim.width / leftDim.height);
                const midW = this.visualHeight * (midDim.width / midDim.height);
                const rightW = this.visualHeight * (rightDim.width / rightDim.height);

                let x = -this.width / 2;
                ctx.drawImage(VOLCANO_BAR_CONFIG.images[parts[0]], x, drawY, leftW, this.visualHeight);
                x += leftW;

                const midSpace = this.width - leftW - rightW;
                const midTiles = Math.max(1, Math.ceil(midSpace / midW));
                for (let i = 0; i < midTiles; i++) {
                    ctx.drawImage(VOLCANO_BAR_CONFIG.images[parts[1]], x + i * midW, drawY, midW, this.visualHeight);
                }

                ctx.drawImage(VOLCANO_BAR_CONFIG.images[parts[2]], -this.width / 2 + this.width - rightW, drawY, rightW, this.visualHeight);
            } else {
                this.drawFallbackPlatform(ctx);
            }
            this.drawMeltOverlay(ctx);
            ctx.restore();
            return;
        }

        if (this.sprite && VOLCANO_BAR_CONFIG.loaded[this.sprite]) {
            const img = VOLCANO_BAR_CONFIG.images[this.sprite];
            if (this.preserveAspect) {
                const dim = VOLCANO_BAR_CONFIG.dimensions[this.sprite];
                if (!dim) {
                    this.drawFallbackPlatform(ctx);
                    this.drawMeltOverlay(ctx);
                    ctx.restore();
                    return;
                }
                const tileW = this.visualHeight * (dim.width / dim.height);
                const tiles = Math.max(1, Math.round(this.width / tileW));
                const totalTileW = tiles * tileW;
                const startX = -totalTileW / 2;
                for (let i = 0; i < tiles; i++) {
                    ctx.drawImage(
                        img,
                        startX + i * tileW,
                        -this.height / 2 + this.visualOffsetY,
                        tileW,
                        this.visualHeight
                    );
                }
            } else {
                ctx.drawImage(
                    img,
                    -this.width / 2,
                    -this.height / 2 + this.visualOffsetY,
                    this.width,
                    this.visualHeight
                );
            }
            this.drawMeltOverlay(ctx);
            ctx.restore();
            return;
        }

        if (this.sprite) {
            this.drawFallbackPlatform(ctx);
            this.drawMeltOverlay(ctx);
            ctx.restore();
            return;
        }

        this.drawFallbackPlatform(ctx);
        this.drawMeltOverlay(ctx);
        ctx.restore();
    }

    drawFallbackPlatform(ctx) {
        ctx.fillStyle = this.color;
        ctx.fillRect(-this.width / 2, -this.height / 2, this.width, this.height);
        ctx.strokeStyle = this.volcano ? '#ffb347' : '#ffffff';
        ctx.lineWidth = 2;
        ctx.strokeRect(-this.width / 2, -this.height / 2, this.width, this.height);

        if (this.volcano) {
            ctx.fillStyle = 'rgba(255, 87, 20, 0.65)';
            ctx.fillRect(-this.width / 2, -this.height / 2 + this.height - 4, this.width, 4);
            ctx.fillStyle = 'rgba(255, 210, 90, 0.35)';
            ctx.fillRect(-this.width / 2, -this.height / 2, this.width, 5);
        } else {
            ctx.fillStyle = 'rgba(255,255,255,0.3)';
            ctx.fillRect(-this.width / 2, -this.height / 2, this.width, 5);
        }
    }

    drawMeltOverlay(ctx) {
        if (!this.isMelting) return;

        const progress = Math.min(1, Math.max(0, this.meltProgress));
        const pulse = 0.55 + Math.sin(Date.now() / 90) * 0.18;

        ctx.save();
        ctx.globalCompositeOperation = 'source-over';
        ctx.shadowColor = '#ff4a00';
        ctx.shadowBlur = 10 + progress * 22;
        ctx.fillStyle = `rgba(255, ${Math.round(80 + 70 * pulse)}, 0, ${0.18 + progress * 0.38})`;
        ctx.fillRect(-this.width / 2, -this.height / 2 - 2, this.width, this.height + this.visualHeight * 0.18);

        ctx.strokeStyle = `rgba(255, 218, 96, ${0.5 + progress * 0.45})`;
        ctx.lineWidth = 2 + progress * 2;
        const crackCount = 3;
        for (let i = 0; i < crackCount; i++) {
            const startX = -this.width / 2 + this.width * ((i + 1) / (crackCount + 1));
            ctx.beginPath();
            ctx.moveTo(startX, -this.height / 2 - 3);
            ctx.lineTo(startX + Math.sin(Date.now() / 180 + i) * 10, this.height / 2 + 5);
            ctx.stroke();
        }

        if (progress > 0.66) {
            ctx.fillStyle = `rgba(255, 80, 0, ${progress - 0.35})`;
            const dripCount = 5;
            for (let i = 0; i < dripCount; i++) {
                const dripX = -this.width / 2 + (this.width / (dripCount + 1)) * (i + 1);
                const dripH = 8 + progress * 18 * (0.6 + Math.sin(Date.now() / 170 + i) * 0.4);
                ctx.beginPath();
                ctx.ellipse(dripX, this.height / 2 + dripH * 0.45, 3 + progress * 3, dripH, 0, 0, Math.PI * 2);
                ctx.fill();
            }
        }
        ctx.restore();
    }
}

// 创建玩家（世界坐标）
function createPlayer(id) {
    return {
        id: id,
        x: 150 + id * 150,
        y: 200,
        vx: 0,
        vy: 0,
        color: CONFIG.colors[id],
        isIT: false,
        size: CONFIG.playerSize,
        onGround: false,
        coyoteTimer: 0,
        jumpBufferTimer: 0,
        // 道具相关
        item: null,
        hasItem: false,
        facingRight: true,
        // 道具效果状态
        speedBoost: false,
        speedBoostTimer: 0,
        speedBoostEffect: 0,    // 速度加成值
        baseSpeed: CONFIG.playerSpeed,
        trackerActive: false,
        trackerTimer: 0,
        speedSlowActive: false,
        speedSlowTimer: 0,
        speedSlowed: false,     // 被减速状态
        speedSlowEffect: 0,     // 减速值
        catchBoostActive: false,
        catchBoostTimer: 0,
        catchBoostEffect: false,
        shieldActive: false,
        shieldTimer: 0,
        usingItem: false,
        // 专属能量球加成
        powerOrbBoost: false,
        powerOrbTimer: 0,
        powerOrbEffect: false,
        jumpBoost: 1,  // 跳跃加成倍率
        // 闪电技能
        hasZap: false,
        zapCooldown: false,
        zapCooldownTimer: 0,
        // 身份互换冷却
        swapCooldown: false,
        swapCooldownTimer: 0,
        // 被电击状态
        zapped: false,
        zappedTimer: 0,
        // 专属能量球加成
        powerOrbBoost: false,
        powerOrbTimer: 0,
        powerOrbEffect: false
    };
}

// 创建平台（世界坐标）
function createPlatforms() {
    const w = gameState.worldWidth;
    const h = gameState.worldHeight;

    if (gameState.currentLevel === 2) {
        createVolcanoPlatforms(w, h);
        return;
    }

    if (gameState.currentLevel === 3) {
        createForestPlatforms(w, h);
        return;
    }

    createCityPlatforms(w, h);
}

function createVolcanoPlatforms(w, h) {
    const bar = (x, y, width, sprite, options = {}) => new Platform(
        x,
        y,
        width,
        18,
        '#4b241d',
        {
            volcano: true,
            sprite,
            visualHeight: options.visualHeight || 58,
            visualOffsetY: options.visualOffsetY ?? -14,
            moveX: options.moveX || 0,
            moveY: options.moveY || 0,
            speed: options.speed || 0,
            phase: options.phase || 0,
            rotationSpeed: options.rotationSpeed || 0,
            rotationRange: options.rotationRange || 0,
            canMelt: options.canMelt !== false
        }
    );

    gameState.platforms = [
        bar(0, h - 28, w, 'stoneMid', { visualHeight: 56, visualOffsetY: -14, canMelt: false }),

        bar(w * 0.04, h - 92, 190, 'stoneTop', { visualHeight: 48, visualOffsetY: -12 }),
        bar(w * 0.24, h - 98, 220, 'lavaPipe', { visualHeight: 42, visualOffsetY: -14 }),
        bar(w * 0.48, h - 94, 210, 'stoneMid', { visualHeight: 48, visualOffsetY: -12 }),
        bar(w * 0.72, h - 100, 190, 'lavaPipe', { visualHeight: 42, visualOffsetY: -14 }),
        bar(w * 0.88, h - 94, 150, 'pipeRight', { visualHeight: 72, visualOffsetY: -24 }),

        bar(w * 0.08, h - 165, 190, 'pipeLeft', { visualHeight: 72, visualOffsetY: -24 }),
        bar(w * 0.28, h - 172, 230, 'stoneTop', { visualHeight: 48, visualOffsetY: -12 }),
        bar(w * 0.52, h - 166, 220, 'lavaPipe', { visualHeight: 42, visualOffsetY: -14, moveX: 42, speed: 0.0017, phase: 0, rotationSpeed: 0.0016, rotationRange: 0.18 }),
        bar(w * 0.76, h - 172, 180, 'stoneMid', { visualHeight: 48, visualOffsetY: -12 }),

        bar(w * 0.02, h - 240, 160, 'lavaPipe', { visualHeight: 42, visualOffsetY: -14 }),
        bar(w * 0.18, h - 250, 210, 'stoneMid', { visualHeight: 48, visualOffsetY: -12 }),
        bar(w * 0.42, h - 244, 240, 'pipeSupported', { visualHeight: 86, visualOffsetY: -30 }),
        bar(w * 0.68, h - 252, 210, 'stoneTop', { visualHeight: 48, visualOffsetY: -12, moveX: 42, speed: 0.0017, phase: Math.PI, rotationSpeed: 0.0016, rotationRange: 0.18 }),
        bar(w * 0.88, h - 244, 140, 'lavaPipe', { visualHeight: 42, visualOffsetY: -14 }),

        bar(w * 0.08, h - 322, 190, 'stoneTop', { visualHeight: 48, visualOffsetY: -12 }),
        bar(w * 0.30, h - 330, 210, 'lavaPipe', { visualHeight: 42, visualOffsetY: -14 }),
        bar(w * 0.52, h - 324, 220, 'stoneMid', { visualHeight: 48, visualOffsetY: -12 }),
        bar(w * 0.74, h - 332, 190, 'pipeRight', { visualHeight: 72, visualOffsetY: -24 }),

        bar(w * 0.02, h - 405, 160, 'pipeLeft', { visualHeight: 72, visualOffsetY: -24 }),
        bar(w * 0.20, h - 412, 220, 'lavaPipe', { visualHeight: 42, visualOffsetY: -14 }),
        bar(w * 0.46, h - 405, 230, 'stoneTop', { visualHeight: 48, visualOffsetY: -12 }),
        bar(w * 0.72, h - 414, 200, 'stoneMid', { visualHeight: 48, visualOffsetY: -12 }),

        bar(w * 0.16, h - 492, 190, 'lavaPipe', { visualHeight: 42, visualOffsetY: -14 }),
        bar(w * 0.40, h - 500, 220, 'stoneMid', { visualHeight: 48, visualOffsetY: -12 }),
        bar(w * 0.66, h - 492, 190, 'stoneTop', { visualHeight: 48, visualOffsetY: -12 }),
    ];

    gameState.springs = [];

    gameState.traps = [];
}

function createCityPlatforms(w, h) {
    const bar = (x, y, width, sprite, options = {}) => new Platform(
        x,
        y,
        width,
        18,
        '#24324a',
        {
            sprite,
            visualHeight: options.visualHeight || 58,
            visualOffsetY: options.visualOffsetY ?? -12,
            moveX: options.moveX || 0,
            moveY: options.moveY || 0,
            speed: options.speed || 0,
            phase: options.phase || 0,
            rotationSpeed: options.rotationSpeed || 0,
            rotationRange: options.rotationRange || 0
        }
    );

    // 底部实线在地图最下方
    const cityGroundY = h - 20;

    gameState.platforms = [
        // 底部实线（视觉装饰，碰撞检测用）
        new Platform(0, cityGroundY, w, 20, '#2d4a6e', { visible: true }),

        // 第1层 - 3个
        bar(w * 0.08, h - 90, 220, 'cityStoneTop', { visualHeight: 58 }),
        bar(w * 0.45, h - 90, 280, 'cityCenterBar', { visualHeight: 58 }),
        bar(w * 0.82, h - 90, 180, 'cityPipeRight', { visualHeight: 58 }),

        // 第2层 - 3个
        bar(w * 0.15, h - 170, 200, 'cityBluePipe', { visualHeight: 58 }),
        bar(w * 0.55, h - 170, 260, 'cityPipeLeft', { visualHeight: 58 }),
        bar(w * 0.88, h - 170, 160, 'cityBluePipe', { visualHeight: 58 }),

        // 第3层 - 3个
        bar(w * 0.05, h - 250, 240, 'cityPipeSupported', { visualHeight: 58 }),
        bar(w * 0.50, h - 250, 220, 'cityStoneTop', { visualHeight: 58 }),
        bar(w * 0.78, h - 250, 200, 'cityBluePipe', { visualHeight: 58 }),

        // 第4层 - 3个
        bar(w * 0.20, h - 330, 200, 'cityWarningBar', { visualHeight: 58 }),
        bar(w * 0.58, h - 330, 240, 'cityCenterBar', { visualHeight: 58 }),
        bar(w * 0.85, h - 330, 180, 'cityPipeRight', { visualHeight: 58 }),

        // 第5层 - 2个（顶部）
        bar(w * 0.35, h - 410, 260, 'cityBluePipe', { visualHeight: 58 }),
        bar(w * 0.72, h - 410, 220, 'cityStoneTop', { visualHeight: 58 }),
    ];

    gameState.springs = [];
    gameState.traps = [];
}

function createForestPlatforms(w, h) {
    const forestBarSprites = ['forestLog01', 'forestLog02', 'forestLog03', 'forestLog04'];
    const randSprite = () => forestBarSprites[Math.floor(Math.random() * forestBarSprites.length)];

    const bar = (x, y, width, sprite, options = {}) => new Platform(
        x,
        y,
        width,
        18,
        '#24324a',
        {
            sprite,
            visualHeight: options.visualHeight || 52,
            visualOffsetY: options.visualOffsetY ?? -12,
            moveX: options.moveX || 0,
            moveY: options.moveY || 0,
            speed: options.speed || 0,
            phase: options.phase || 0,
            rotationSpeed: options.rotationSpeed || 0,
            rotationRange: options.rotationRange || 0,
            preserveAspect: options.preserveAspect || false,
            compositeSprite: options.compositeSprite || null
        }
    );

    const swingBar = (x, y, width, sprite, options = {}) => bar(x, y, width, sprite, {
        ...options,
        moveX: options.moveX ?? 25,
        speed: options.speed ?? 0.001,
        phase: options.phase ?? Math.random() * Math.PI * 2
    });

    gameState.platforms = [
        bar(0, h - 28, w, null, {
            compositeSprite: ['forestLog05L', 'forestLog05M', 'forestLog05R'],
            visualHeight: 80,
            visualOffsetY: -31
        }),

        swingBar(w * 0.02, h - 110, 280, 'forestLog01', { preserveAspect: true, visualHeight: 48, phase: 0 }),
        bar(w * 0.45, h - 120, 260, randSprite(), { preserveAspect: true, visualHeight: 48 }),

        bar(w * 0.20, h - 210, 240, randSprite(), { preserveAspect: true, visualHeight: 48 }),
        swingBar(w * 0.62, h - 220, 280, 'forestLog01', { preserveAspect: true, visualHeight: 48, phase: 1.5 }),

        swingBar(w * 0.02, h - 310, 260, 'forestLog01', { preserveAspect: true, visualHeight: 48, phase: 3.0 }),
        bar(w * 0.40, h - 320, 250, randSprite(), { preserveAspect: true, visualHeight: 48 }),
        bar(w * 0.72, h - 310, 240, randSprite(), { preserveAspect: true, visualHeight: 48 }),

        bar(w * 0.18, h - 410, 260, randSprite(), { preserveAspect: true, visualHeight: 48 }),
        swingBar(w * 0.58, h - 420, 280, 'forestLog01', { preserveAspect: true, visualHeight: 48, phase: 4.5 }),
    ];

    gameState.springs = [];
    gameState.traps = [];
}

// 更新玩家
function updatePlayer(p, index, dt) {
    const ctrl = CONTROLS[index];

    // 定身状态：速度清零
    if (p.zapped) {
        p.vx = 0;
        p.vy = 0;
        // 更新定身计时器
        p.zappedTimer -= dt;
        if (p.zappedTimer <= 0) {
            p.zapped = false;
        }
        // 仍然更新位置（因为有重力），但不接受输入
        p.vy += CONFIG.gravity;
        p.y += p.vy;
        return;
    }

    // 计算实际移动速度（基于道具效果）
    let actualSpeed = CONFIG.playerSpeed;
    if (p.speedBoost) {
        actualSpeed = p.speedBoostEffect > 0 ? p.speedBoostEffect : CONFIG.playerSpeed * 2;
    }
    if (p.speedSlowed) actualSpeed = p.speedSlowEffect;
    if (p.catchBoostActive) actualSpeed = CONFIG.playerSpeed * 2.5; // 鬼冲刺2.5倍速度

    // 水平移动
    p.vx = 0;
    if (keys[ctrl.left]) {
        p.vx = -actualSpeed;
        p.facingRight = false;
    }
    if (keys[ctrl.right]) {
        p.vx = actualSpeed;
        p.facingRight = true;
    }

    // 道具现在碰到自动使用，不需要按键

    // 电击技能（按zap键发射闪电）
    if (keys[ctrl.zap] && p.hasZap && !p.zapCooldown) {
        const startX = p.x;
        const endX = p.facingRight ? gameState.worldWidth : 0;
        const zap = new ZapEffect(startX, endX, p.y);
        gameState.zapEffects.push(zap);

        // 检测沿途的玩家
        for (let other of gameState.players) {
            if (other === p) continue;
            const inLine = p.facingRight
                ? (other.x > startX && other.x < endX)
                : (other.x < startX && other.x > endX);
            const sameHeight = Math.abs(other.y - p.y) < 50;
            if (inLine && sameHeight) {
                other.zapped = true;
                other.zappedTimer = 2000; // 2秒定身
            }
        }

        p.hasZap = false;
        p.zapCooldown = true;
        p.zapCooldownTimer = 25000; // 25秒冷却
        playSound();
    }

    // 更新电击冷却
    if (p.zapCooldown) {
        p.zapCooldownTimer -= 16.67;
        if (p.zapCooldownTimer <= 0) {
            p.zapCooldown = false;
        }
    }

    // 跳跃缓冲
    if (keys[ctrl.jump] && p.jumpBufferTimer === 0) {
        p.jumpBufferTimer = CONFIG.jumpBuffer;
    }

    // 跳跃判定
    const canJump = p.onGround || p.coyoteTimer > 0;
    const wantsJump = p.jumpBufferTimer > 0;

    if (canJump && wantsJump) {
        // 专属能量球加成：跳跃力提升30%
        const jumpBoost = p.powerOrbBoost && p.jumpBoost ? p.jumpBoost : 1;
        const levelJumpMult = gameState.currentLevel === 3 ? 1.5 : 1;
        p.vy = -CONFIG.jumpSpeed * jumpBoost * levelJumpMult;
        p.onGround = false;
        p.coyoteTimer = 0;
        p.jumpBufferTimer = 0;
    }

    // 重力（缓降时减半）
    const actualGravity = p.slowFall ? CONFIG.gravity * 0.4 : CONFIG.gravity;
    p.vy += actualGravity;

    // 限制下落速度（缓降时更慢）
    const maxFallSpeed = p.slowFall ? 8 : 15;
    if (p.vy > maxFallSpeed) p.vy = maxFallSpeed;

    // 更新位置
    p.x += p.vx;
    p.y += p.vy;

    // 世界边界限制（不能超出画布）
    if (p.x < p.size) p.x = p.size;
    if (p.x > gameState.worldWidth - p.size) p.x = gameState.worldWidth - p.size;

    // 记录跳跃前是否在地上
    const wasOnGround = p.onGround;

    // 平台碰撞
    p.onGround = false;
    for (let plat of gameState.platforms) {
        if (isPlatformSolid(plat) &&
            p.vy >= 0 &&
            p.x + p.size > plat.x &&
            p.x - p.size < plat.x + plat.width &&
            p.y + p.size > plat.y &&
            p.y + p.size < plat.y + plat.height + 10) {
            p.y = plat.y - p.size;
            p.vy = 0;
            p.onGround = true;
        }
    }

    // 土狼时间
    if (wasOnGround && !p.onGround && p.vy >= 0) {
        p.coyoteTimer = CONFIG.coyoteTime;
    }

    // 弹簧碰撞检测
    for (let spring of gameState.springs) {
        if (spring.checkCollision(p)) {
            p.vy = -SPRING_POWER;  // 超级跳跃
            p.onGround = false;
            p.slowFall = true;  // 开启缓降
            p.slowFallTimer = 60;  // 缓降持续约1秒
        }
    }

    // 补鼠夹碰撞检测
    for (let trap of gameState.traps) {
        if (trap.checkCollision(p) && !p.zapped) {
            p.zapped = true;
            p.zappedTimer = TRAP_STUN_DURATION;
            trap.active = false;  // 碰到后自动消失
        }
    }
    // 清理已触发的补鼠夹
    gameState.traps = gameState.traps.filter(t => t.active);

    // 缓降效果
    if (p.slowFall) {
        p.slowFallTimer--;
        if (p.slowFallTimer <= 0) {
            p.slowFall = false;
        }
    }

    // 更新计时器
    if (p.coyoteTimer > 0) p.coyoteTimer--;
    if (p.jumpBufferTimer > 0) p.jumpBufferTimer--;

    // 更新道具效果
    updateItemEffects(p, dt);

    // 掉落到底部重生（不超出世界）
    if (p.y > gameState.worldHeight + 50) {
        p.y = 50;
        p.x = 150 + p.id * 150;
        p.vy = 0;
        p.coyoteTimer = 0;
        p.jumpBufferTimer = 0;
    }
}

// 小猫角色配置（4只独立PNG）
const POKEMON_CONFIG = {
    images: [],           // 4只宝可梦的图片数组
    imageLoaded: [false, false, false, false],  // 各自加载状态
    currentFrame: 0,
    frameTimer: 0,
    frameInterval: 400    // 动画间隔(ms)
};

// 加载4只小猫独立图片
function loadPokemonSprites() {
    const pokemonNames = ['cat_p1_yellow', 'cat_p2_pink', 'cat_p3_blue', 'cat_p4_purple'];

    pokemonNames.forEach((name, index) => {
        const img = new Image();
        img.onload = () => {
            POKEMON_CONFIG.imageLoaded[index] = true;
            console.log(`角色 ${name} 加载成功`);
        };
        img.onerror = () => {
            console.log(`角色 ${name} 加载失败`);
        };
        img.src = `assets/${name}.png`;
        POKEMON_CONFIG.images[index] = img;
    });
}

function loadVolcanoBarSprites() {
    Object.entries(VOLCANO_BAR_CONFIG.files).forEach(([key, file]) => {
        const img = new Image();
        img.onload = () => {
            VOLCANO_BAR_CONFIG.loaded[key] = true;
            VOLCANO_BAR_CONFIG.dimensions[key] = {
                width: img.naturalWidth,
                height: img.naturalHeight
            };
        };
        img.onerror = () => {
            console.log(`横杠素材 ${file} 加载失败`);
        };
        img.src = `assets/${file}`;
        VOLCANO_BAR_CONFIG.images[key] = img;
    });
}

function loadCityAttackSprites() {
    Object.entries(CITY_ATTACK_ASSETS).forEach(([key, paths]) => {
        const candidates = Array.isArray(paths) ? paths : [paths];
        let index = 0;

        const tryLoad = () => {
            if (index >= candidates.length) {
                CITY_ATTACK_IMAGES[key].loaded = false;
                console.log(`城市事件素材 ${key} 加载失败，使用备用图形`);
                return;
            }

            const img = new Image();
            img.onload = () => {
                CITY_ATTACK_IMAGES[key].loaded = true;
                CITY_ATTACK_IMAGES[key].image = img;
            };
            img.onerror = () => {
                index++;
                tryLoad();
            };
            img.src = candidates[index];
            CITY_ATTACK_IMAGES[key].image = img;
        };

        tryLoad();
    });
}

// 绘制玩家
function drawPlayer(ctx, p, offsetY) {
    const drawY = p.y - offsetY;

    // 如果宝可梦图片加载成功，使用独立图片
    if (POKEMON_CONFIG.imageLoaded[p.id]) {
        const img = POKEMON_CONFIG.images[p.id];

        // 取完整小猫本体，包括脚；底部贴住玩家碰撞脚点。
        const sourceX = 10;
        const sourceY = 24;
        const sourceW = 236;
        const sourceH = 216;
        const drawWidth = p.size * 3.0;
        const drawHeight = p.size * 2.75;

        // 上下弹跳动画
        const bobOffset = Date.now() / POKEMON_CONFIG.frameInterval;
        const bobY = p.onGround ? 0 : Math.sin(bobOffset * 1.5) * 2;

        // 是否被标记(鬼)的特效
        if (p.isIT) {
            const isWhiteGlow = gameState.currentLevel === 2 || gameState.currentLevel === 3;
            ctx.shadowColor = isWhiteGlow ? '#ffffff' : '#ff0000';
            ctx.shadowBlur = 20 + Math.sin(Date.now() / 100) * 10;
        }

        // 速度提升效果
        if (p.speedBoost || p.catchBoostActive) {
            ctx.shadowColor = '#ffeb3b';
            ctx.shadowBlur = 25;
        }

        // 专属能量球加成效果
        if (p.powerOrbBoost) {
            ctx.shadowColor = p.color;
            ctx.shadowBlur = 40;
        }

        // 护盾效果
        if (p.shieldActive) {
            ctx.beginPath();
            ctx.arc(p.x, drawY + bobY, p.size + 15, 0, Math.PI * 2);
            ctx.strokeStyle = '#2196f3';
            ctx.lineWidth = 4;
            ctx.setLineDash([5, 5]);
            ctx.stroke();
            ctx.setLineDash([]);
        }

        // 绘制角色图片：p.y 是碰撞中心，p.y + p.size 是脚底。
        const footY = drawY + p.size;
        const drawX = p.x - drawWidth / 2;
        const imageY = footY - drawHeight + bobY + 4;
        ctx.drawImage(
            img,
            sourceX, sourceY, sourceW, sourceH,
            drawX, imageY,
            drawWidth, drawHeight
        );

        ctx.shadowBlur = 0;

        // 专属能量球加成 - 翅膀效果
        if (p.powerOrbBoost) {
            const time = Date.now() / 200;
            const wingColor = p.id === 0 ? '#ff6b6b' : p.id === 1 ? '#54a0ff' : p.id === 2 ? '#feca57' : '#1dd1a1';

            // 翅膀
            ctx.save();
            ctx.translate(p.x - p.size * 1.2, drawY + bobY);
            ctx.rotate(Math.sin(time) * 0.3 - 0.5);
            ctx.beginPath();
            ctx.ellipse(0, 0, 20, 12, 0, 0, Math.PI * 2);
            ctx.fillStyle = wingColor;
            ctx.globalAlpha = 0.7;
            ctx.fill();
            ctx.restore();

            ctx.save();
            ctx.translate(p.x + p.size * 1.2, drawY + bobY);
            ctx.rotate(-Math.sin(time) * 0.3 + 0.5);
            ctx.beginPath();
            ctx.ellipse(0, 0, 20, 12, 0, 0, Math.PI * 2);
            ctx.fillStyle = wingColor;
            ctx.globalAlpha = 0.7;
            ctx.fill();
            ctx.restore();

            ctx.globalAlpha = 1;

            // 能量标识
            ctx.fillStyle = '#ffd700';
            ctx.font = 'bold 12px Arial';
            ctx.textAlign = 'center';
            ctx.fillText('⚡', p.x, drawY - p.size * 2.2 + bobY);
        }

        // 专属能量球加成 - 模型变化 + 发光
        if (p.powerOrbBoost) {
            // 外层光环
            ctx.beginPath();
            ctx.arc(p.x, drawY + bobY, p.size + 10, 0, Math.PI * 2);
            const glowColor = p.id === 0 ? 'rgba(255,107,107,0.5)' :
                             p.id === 1 ? 'rgba(84,160,255,0.5)' :
                             p.id === 2 ? 'rgba(254,202,87,0.5)' : 'rgba(29,209,161,0.5)';
            const orbGradient = ctx.createRadialGradient(
                p.x, drawY + bobY, p.size,
                p.x, drawY + bobY, p.size + 10
            );
            orbGradient.addColorStop(0, glowColor);
            orbGradient.addColorStop(1, 'rgba(255,255,255,0)');
            ctx.fillStyle = orbGradient;
            ctx.fill();
        }

        return; // 结束，不再绘制圆形
    }

    // ===== 回退：没有图片时绘制圆形角色 =====

    // 护盾效果
    if (p.shieldActive) {
        ctx.beginPath();
        ctx.arc(p.x, drawY, p.size + 10, 0, Math.PI * 2);
        ctx.strokeStyle = '#2196f3';
        ctx.lineWidth = 4;
        ctx.setLineDash([5, 5]);
        ctx.stroke();
        ctx.setLineDash([]);
    }

    // 专属能量球加成效果 - 模型变化 + 发光
    if (p.powerOrbBoost) {
        // 外层光环
        ctx.beginPath();
        ctx.arc(p.x, drawY, p.size + 15, 0, Math.PI * 2);
        const orbGradient = ctx.createRadialGradient(
            p.x, drawY, p.size,
            p.x, drawY, p.size + 15
        );
        const glowColor = p.id === 0 ? 'rgba(0,255,255,0.5)' :
                         p.id === 1 ? 'rgba(255,0,255,0.5)' :
                         p.id === 2 ? 'rgba(255,255,0,0.5)' : 'rgba(0,255,0,0.5)';
        orbGradient.addColorStop(0, glowColor);
        orbGradient.addColorStop(1, 'rgba(255,255,255,0)');
        ctx.fillStyle = orbGradient;
        ctx.fill();

        // 发光效果
        ctx.shadowColor = glowColor.replace('0.5', '1');
        ctx.shadowBlur = 40;

        // 旋转的能量环
        const time = Date.now() / 100;
        ctx.save();
        ctx.translate(p.x, drawY);
        ctx.rotate(time);
        ctx.strokeStyle = glowColor.replace('0.5', '0.8');
        ctx.lineWidth = 2;
        ctx.setLineDash([8, 4]);
        ctx.beginPath();
        ctx.arc(0, 0, p.size + 12, 0, Math.PI * 2);
        ctx.stroke();
        ctx.setLineDash([]);
        ctx.restore();

        ctx.shadowBlur = 0;
    }

    // 速度提升效果
    if (p.speedBoost || p.catchBoostActive) {
        ctx.shadowColor = '#ffeb3b';
        ctx.shadowBlur = 25;
    }

    // 被电击效果 - 闪烁
    if (p.zapped) {
        ctx.globalAlpha = 0.5 + Math.sin(Date.now() / 50) * 0.5;
    }

    // 玩家本体
    ctx.beginPath();
    ctx.arc(p.x, drawY, p.size, 0, Math.PI * 2);

    if (p.isIT) {
        ctx.fillStyle = '#ffffff';
        const isWhiteGlow = gameState.currentLevel === 2 || gameState.currentLevel === 3;
        ctx.shadowColor = isWhiteGlow ? '#ffffff' : '#ff0000';
        ctx.shadowBlur = 20;
    } else {
        ctx.fillStyle = p.color;
    }

    ctx.fill();
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 3;
    ctx.stroke();

    ctx.shadowBlur = 0;
    ctx.globalAlpha = 1;
    ctx.fillStyle = '#000';
    ctx.font = 'bold 16px Arial';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('P' + (p.id + 1), p.x, drawY);

    // 专属能量球加成 - 模型变化（翅膀/能量翼）
    if (p.powerOrbBoost) {
        const time = Date.now() / 200;
        const wingColor = p.id === 0 ? '#00ffff' :
                         p.id === 1 ? '#ff00ff' :
                         p.id === 2 ? '#ffff00' : '#00ff00';

        ctx.shadowColor = wingColor;
        ctx.shadowBlur = 15;

        // 左侧翅膀
        ctx.save();
        ctx.translate(p.x - p.size, drawY);
        ctx.rotate(Math.sin(time) * 0.3 - 0.5);
        ctx.beginPath();
        ctx.moveTo(0, 0);
        ctx.quadraticCurveTo(-25, -15, -35, 5);
        ctx.quadraticCurveTo(-20, 15, 0, 10);
        ctx.fillStyle = wingColor;
        ctx.globalAlpha = 0.8;
        ctx.fill();
        ctx.restore();

        // 右侧翅膀
        ctx.save();
        ctx.translate(p.x + p.size, drawY);
        ctx.rotate(-Math.sin(time) * 0.3 + 0.5);
        ctx.beginPath();
        ctx.moveTo(0, 0);
        ctx.quadraticCurveTo(25, -15, 35, 5);
        ctx.quadraticCurveTo(20, 15, 0, 10);
        ctx.fillStyle = wingColor;
        ctx.globalAlpha = 0.8;
        ctx.fill();
        ctx.restore();

        ctx.shadowBlur = 0;
        ctx.globalAlpha = 1;

        // 能量球特效图标
        ctx.fillStyle = '#ffd700';
        ctx.font = 'bold 12px Arial';
        ctx.textAlign = 'center';
        ctx.fillText('⚡', p.x, drawY - p.size - 20);
    }

    // 被电击时显示电击图标
    if (p.zapped) {
        ctx.fillStyle = '#ffd700';
        ctx.font = 'bold 20px Arial';
        ctx.fillText('⚡', p.x, drawY - p.size - 15);
    }
}

// 碰撞检测
function checkCollision() {
    // 道具拾取检测（碰到自动使用）
    const itemCount = gameState.items.length;
    if (itemCount > 0) {
        console.log('当前场景有', itemCount, '个道具');
    }
    for (let i = gameState.items.length - 1; i >= 0; i--) {
        const item = gameState.items[i];
        if (!item.active) continue;

        for (let p of gameState.players) {
            const dx = p.x - item.x;
            const dy = p.y - item.y;
            const dist = Math.sqrt(dx * dx + dy * dy);
            const collisionDist = p.size + item.size;

            if (dist < collisionDist) {
                console.log('拾取道具:', item.type, '玩家:', p.id, '距离:', dist.toFixed(2), '碰撞距离:', collisionDist);
                // 碰到自动使用道具
                useItem(p, item.type);
                item.active = false;
                gameState.items.splice(i, 1);
                break;
            }
        }
    }

    // 专属能量球拾取检测
    for (let i = gameState.powerOrbs.length - 1; i >= 0; i--) {
        const orb = gameState.powerOrbs[i];
        if (!orb.active) continue;

        for (let p of gameState.players) {
            if (orb.checkCollision(p)) {
                // 拾取专属能量球 - 激活加成
                activatePowerOrbBoost(p);
                orb.active = false;
                gameState.powerOrbs.splice(i, 1);
                playSound();
                break;
            } else if (p.id !== orb.targetPlayerId && orb.checkCollision(p)) {
                // 错误玩家拾取 - 能量球失效消失
                orb.active = false;
                gameState.powerOrbs.splice(i, 1);
                // 显示短暂特效提示
                showOrbFailEffect(p.x, p.y);
            }
        }
    }

    // 玩家碰撞检测
    for (let i = 0; i < gameState.players.length; i++) {
        for (let j = i + 1; j < gameState.players.length; j++) {
            const p1 = gameState.players[i];
            const p2 = gameState.players[j];

            const dx = p1.x - p2.x;
            const dy = p1.y - p2.y;
            const dist = Math.sqrt(dx * dx + dy * dy);

            if (dist < p1.size + p2.size) {
                // 护盾检测
                if (p1.shieldActive) {
                    p1.shieldActive = false;
                    playSound();
                    continue;
                }
                if (p2.shieldActive) {
                    p2.shieldActive = false;
                    playSound();
                    continue;
                }
                handleTag(p1, p2);
            }
        }
    }
}

// 标记特效数组
let tagEffects = [];

// 创建标记特效
function createTagEffect(x, y) {
    tagEffects.push({
        x: x,
        y: y,
        radius: 10,
        maxRadius: 80,
        alpha: 1,
        lifetime: 20
    });
}

// 绘制标记特效
function drawTagEffects(ctx, offsetY) {
    for (let i = tagEffects.length - 1; i >= 0; i--) {
        const effect = tagEffects[i];

        // 外圈光环
        ctx.beginPath();
        ctx.arc(effect.x, effect.y - offsetY, effect.radius, 0, Math.PI * 2);
        ctx.strokeStyle = `rgba(255, 0, 0, ${effect.alpha * 0.8})`;
        ctx.lineWidth = 4;
        ctx.stroke();

        // 内圈光环
        ctx.beginPath();
        ctx.arc(effect.x, effect.y - offsetY, effect.radius * 0.6, 0, Math.PI * 2);
        ctx.strokeStyle = `rgba(255, 255, 0, ${effect.alpha * 0.6})`;
        ctx.lineWidth = 3;
        ctx.stroke();

        // 中心闪光
        if (effect.alpha > 0.5) {
            ctx.beginPath();
            ctx.arc(effect.x, effect.y - offsetY, effect.radius * 0.3, 0, Math.PI * 2);
            ctx.fillStyle = `rgba(255, 255, 255, ${effect.alpha})`;
            ctx.fill();
        }

        // 更新特效
        effect.radius += 4;
        effect.alpha -= 0.05;
        effect.lifetime--;

        if (effect.lifetime <= 0) {
            tagEffects.splice(i, 1);
        }
    }
}

// 处理标记 - 只有IT玩家超过非IT玩家时才触发
function handleTag(p1, p2) {
    const itPlayer = p1.isIT ? p1 : (p2.isIT ? p2 : null);
    const otherPlayer = p1.isIT ? p2 : (p2.isIT ? p1 : null);

    if (!itPlayer || !otherPlayer) return;

    // 检查标记冷却时间
    if (itPlayer.lastTagTime && Date.now() - itPlayer.lastTagTime < CONFIG.tagCooldown) {
        return;
    }

    // 计算相对速度：IT玩家相对于非IT玩家的速度
    const relVx = itPlayer.vx - otherPlayer.vx;
    const relVy = itPlayer.vy - otherPlayer.vy;

    // 只有当IT玩家有正向相对速度（正在接近）时才判定
    // 水平方向：如果IT玩家的vx方向使得距离在减小
    const dx = itPlayer.x - otherPlayer.x;
    const approaching = (relVx * dx > 0) || (relVy * (itPlayer.y - otherPlayer.y) > 0);

    if (approaching) {
        // 创建标记特效
        const midX = (p1.x + p2.x) / 2;
        const midY = (p1.y + p2.y) / 2;
        createTagEffect(midX, midY);

        if (p1.isIT && !p2.isIT) {
            p1.isIT = false;
            p2.isIT = true;
            p2.lastTagTime = Date.now();
            playSound();
            updateITIndicator();
        } else if (!p1.isIT && p2.isIT) {
            p2.isIT = false;
            p1.isIT = true;
            p1.lastTagTime = Date.now();
            playSound();
            updateITIndicator();
        }
    }
}

// 激活专属能量球加成
function activatePowerOrbBoost(p) {
    p.powerOrbBoost = true;
    p.powerOrbTimer = POWER_ORB_CONFIG.effectDuration;
    p.powerOrbEffect = true;
    p.speedBoost = true;
    p.speedBoostTimer = POWER_ORB_CONFIG.effectDuration;
    p.speedBoostEffect = CONFIG.playerSpeed * POWER_ORB_CONFIG.speedBoostAmount;
    p.baseSpeed = CONFIG.playerSpeed;
    p.jumpBoost = POWER_ORB_CONFIG.jumpBoostAmount;

    // 创建拾取特效
    createPowerOrbPickupEffect(p.x, p.y, p.color);
}

// 能量球拾取特效（粒子爆发）
let powerOrbEffects = [];
function createPowerOrbPickupEffect(x, y, color) {
    const effect = {
        x: x,
        y: y,
        color: color,
        particles: [],
        lifetime: 30,
        maxLifetime: 30
    };

    // 创建粒子
    for (let i = 0; i < 12; i++) {
        const angle = (i / 12) * Math.PI * 2;
        effect.particles.push({
            x: x,
            y: y,
            vx: Math.cos(angle) * 5,
            vy: Math.sin(angle) * 5,
            size: 4 + Math.random() * 3
        });
    }

    powerOrbEffects.push(effect);
}

// 绘制能量球拾取特效
function drawPowerOrbEffects(ctx, offsetY) {
    for (let i = powerOrbEffects.length - 1; i >= 0; i--) {
        const effect = powerOrbEffects[i];

        const alpha = effect.lifetime / effect.maxLifetime;

        for (let p of effect.particles) {
            ctx.beginPath();
            ctx.arc(p.x, p.y - offsetY, p.size * alpha, 0, Math.PI * 2);
            ctx.fillStyle = effect.color;
            ctx.globalAlpha = alpha;
            ctx.fill();

            // 更新粒子位置
            p.x += p.vx;
            p.y += p.vy;
            p.vx *= 0.95;
            p.vy *= 0.95;
        }

        ctx.globalAlpha = 1;
        effect.lifetime--;

        if (effect.lifetime <= 0) {
            powerOrbEffects.splice(i, 1);
        }
    }
}

// 显示错误拾取特效
let orbFailEffects = [];
function showOrbFailEffect(x, y) {
    orbFailEffects.push({
        x: x,
        y: y,
        lifetime: 30,
        maxLifetime: 30
    });
}

// 绘制错误拾取特效
function drawOrbFailEffects(ctx, offsetY) {
    for (let i = orbFailEffects.length - 1; i >= 0; i--) {
        const effect = orbFailEffects[i];
        const alpha = effect.lifetime / effect.maxLifetime;

        // 显示 X 标记
        ctx.strokeStyle = `rgba(255,0,0,${alpha})`;
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.moveTo(effect.x - 10, effect.y - offsetY - 10);
        ctx.lineTo(effect.x + 10, effect.y - offsetY + 10);
        ctx.moveTo(effect.x + 10, effect.y - offsetY - 10);
        ctx.lineTo(effect.x - 10, effect.y - offsetY + 10);
        ctx.stroke();

        ctx.font = 'bold 16px Arial';
        ctx.textAlign = 'center';
        ctx.fillStyle = `rgba(255,255,255,${alpha})`;
        ctx.fillText('专属!', effect.x, effect.y - offsetY - 20);

        effect.lifetime--;
        if (effect.lifetime <= 0) {
            orbFailEffects.splice(i, 1);
        }
    }
}

// 道具特效数组
let itemEffects = [];

// 创建道具使用特效
function createItemEffect(p, type, optX, optY) {
    // 传送特效特殊处理
    if (type === 'teleportStart' || type === 'teleportEnd') {
        const effect = {
            x: optX !== undefined ? optX : p.x,
            y: optY !== undefined ? optY : p.y,
            color: '#9c27b0',
            type: type,
            lifetime: 30,
            maxLifetime: 30,
            radius: 0,
            maxRadius: type === 'teleportStart' ? 50 : 60
        };
        itemEffects.push(effect);
        return;
    }

    const config = ITEMS[type];
    if (!config) return;

    const effect = {
        x: p.x,
        y: p.y,
        color: config.color,
        type: type,
        lifetime: 40,
        maxLifetime: 40,
        particles: []
    };

    // 根据道具类型创建不同特效
    switch (type) {
        case 'smokeBomb': // 隐身 - 烟雾效果
            for (let i = 0; i < 15; i++) {
                effect.particles.push({
                    x: p.x + (Math.random() - 0.5) * 40,
                    y: p.y + (Math.random() - 0.5) * 40,
                    vx: (Math.random() - 0.5) * 3,
                    vy: -Math.random() * 2,
                    size: 8 + Math.random() * 8,
                    alpha: 1
                });
            }
            break;

        case 'speedBoots': // 加速 - 速度线
            for (let i = 0; i < 8; i++) {
                effect.particles.push({
                    x: p.x,
                    y: p.y,
                    vx: -p.facingRight ? (2 + Math.random() * 3) : -(2 + Math.random() * 3),
                    vy: (Math.random() - 0.5) * 2,
                    size: 3,
                    alpha: 1,
                    length: 15 + Math.random() * 10
                });
            }
            break;

        case 'teleportScroll': // 瞬移 - 传送门效果
            effect.radius = 0;
            effect.maxRadius = 50;
            break;

        case 'trackerScope': // 透视 - 扫描线
            effect.scanAngle = 0;
            break;

        case 'speedSlow': // 减速 - 冰冻效果
            for (let i = 0; i < 12; i++) {
                const angle = (i / 12) * Math.PI * 2;
                effect.particles.push({
                    x: p.x,
                    y: p.y,
                    vx: Math.cos(angle) * 4,
                    vy: Math.sin(angle) * 4,
                    size: 5,
                    alpha: 1
                });
            }
            break;

        case 'catchBoost': // 冲刺 - 鬼影效果
            for (let i = 0; i < 5; i++) {
                effect.particles.push({
                    x: p.x - i * 15 * (p.facingRight ? 1 : -1),
                    y: p.y,
                    size: p.size - i * 3,
                    alpha: 0.5 - i * 0.1
                });
            }
            break;

        case 'timeBonus': // 延时 - 时间加号特效
            effect.timeBonus = true;
            effect.rotation = 0;
            break;

        case 'shield': // 护盾 - 能量盾展开
            effect.radius = 0;
            effect.maxRadius = p.size + 15;
            break;

        case 'zapGun': // 电击 - 电弧效果
            for (let i = 0; i < 10; i++) {
                const angle = Math.random() * Math.PI * 2;
                effect.particles.push({
                    x: p.x,
                    y: p.y,
                    vx: Math.cos(angle) * 5,
                    vy: Math.sin(angle) * 5,
                    size: 2,
                    alpha: 1
                });
            }
            break;
    }

    itemEffects.push(effect);
}

// 绘制全局道具特效
function drawAllItemEffects(ctx, offsetY) {
    for (let i = itemEffects.length - 1; i >= 0; i--) {
        const effect = itemEffects[i];
        const alpha = effect.lifetime / effect.maxLifetime;
        const drawY = effect.y - offsetY;

        switch (effect.type) {
            case 'speedBoots': // 速度线
                ctx.strokeStyle = `rgba(255,235,59,${alpha})`;
                ctx.lineWidth = 2;
                for (let p of effect.particles) {
                    ctx.beginPath();
                    ctx.moveTo(p.x, p.y - offsetY);
                    ctx.lineTo(p.x + p.vx * p.length / 3, p.y - offsetY + p.vy * p.length / 3);
                    ctx.stroke();
                    p.x += p.vx;
                    p.y += p.vy;
                }
                break;

            case 'teleportStart': // 传送起点 - 收缩消失
            case 'teleportEnd': // 传送终点 - 扩散出现
                effect.radius += effect.type === 'teleportStart' ? -4 : 4;
                if (effect.radius > 0) {
                    ctx.beginPath();
                    ctx.arc(effect.x, drawY, Math.abs(effect.radius), 0, Math.PI * 2);
                    ctx.strokeStyle = `rgba(156,39,176,${alpha})`;
                    ctx.lineWidth = 4;
                    ctx.stroke();
                    ctx.beginPath();
                    ctx.arc(effect.x, drawY, Math.abs(effect.radius) * 0.6, 0, Math.PI * 2);
                    ctx.strokeStyle = `rgba(255,255,255,${alpha})`;
                    ctx.lineWidth = 2;
                    ctx.stroke();
                }
                break;

            case 'trackerScope': // 透视扫描
                effect.scanAngle += 0.2;
                ctx.save();
                ctx.translate(effect.x, drawY);
                ctx.rotate(effect.scanAngle);
                ctx.strokeStyle = `rgba(233,30,99,${alpha})`;
                ctx.lineWidth = 3;
                ctx.beginPath();
                ctx.moveTo(0, 0);
                ctx.lineTo(150, 0);
                ctx.stroke();
                ctx.restore();
                break;

            case 'speedSlow': // 冰冻粒子
                for (let p of effect.particles) {
                    ctx.beginPath();
                    ctx.arc(p.x, p.y - offsetY, p.size, 0, Math.PI * 2);
                    ctx.fillStyle = `rgba(100,181,246,${alpha})`;
                    ctx.fill();
                    p.x += p.vx;
                    p.y += p.vy;
                    p.vx *= 0.95;
                    p.vy *= 0.95;
                }
                break;

            case 'catchBoost': // 鬼影
                for (let i = 0; i < effect.particles.length; i++) {
                    const p = effect.particles[i];
                    ctx.beginPath();
                    ctx.arc(p.x, p.y - offsetY, p.size, 0, Math.PI * 2);
                    ctx.fillStyle = `rgba(244,67,53,${alpha * p.alpha})`;
                    ctx.fill();
                }
                break;

            case 'timeStop': // 时间裂缝
                ctx.strokeStyle = `rgba(0,188,212,${alpha})`;
                ctx.lineWidth = 3;
                for (let crack of effect.cracks) {
                    ctx.beginPath();
                    ctx.moveTo(effect.x, drawY);
                    ctx.lineTo(
                        effect.x + Math.cos(crack.angle) * crack.length * alpha,
                        drawY + Math.sin(crack.angle) * crack.length * alpha
                    );
                    ctx.stroke();
                }
                break;

            case 'shield': // 能量盾
                effect.radius += 5;
                ctx.beginPath();
                ctx.arc(effect.x, drawY, effect.radius, 0, Math.PI * 2);
                ctx.strokeStyle = `rgba(33,150,243,${alpha})`;
                ctx.lineWidth = 4;
                ctx.stroke();
                if (effect.radius > effect.maxRadius) {
                    ctx.beginPath();
                    ctx.arc(effect.x, drawY, effect.maxRadius, 0, Math.PI * 2);
                    ctx.setLineDash([5, 5]);
                    ctx.stroke();
                    ctx.setLineDash([]);
                }
                break;

            case 'timeBonus': // 延时 - 向上飘的+10
                effect.rotation = (effect.rotation || 0) + 0.1;
                ctx.save();
                ctx.translate(effect.x, drawY);
                ctx.rotate(Math.sin(effect.rotation) * 0.2);
                ctx.font = 'bold 24px Arial';
                ctx.fillStyle = `rgba(0,188,212,${alpha})`;
                ctx.textAlign = 'center';
                ctx.fillText('+10', 0, -alpha * 30);
                ctx.restore();
                break;

            case 'zapGun': // 电弧
                ctx.strokeStyle = `rgba(255,215,0,${alpha})`;
                ctx.lineWidth = 2;
                for (let p of effect.particles) {
                    ctx.beginPath();
                    ctx.moveTo(p.x, p.y - offsetY);
                    ctx.lineTo(p.x + p.vx * 3, p.y - offsetY + p.vy * 3);
                    ctx.stroke();
                    p.x += p.vx;
                    p.y += p.vy;
                    p.vx *= 0.9;
                    p.vy *= 0.9;
                }
                break;
        }

        effect.lifetime--;
        if (effect.lifetime <= 0) {
            itemEffects.splice(i, 1);
        }
    }
}

// 音效
function playSound() {
    try {
        const ctx = new (window.AudioContext || window.webkitAudioContext)();
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.frequency.value = 880;
        gain.gain.value = 0.3;
        osc.start();
        osc.stop(ctx.currentTime + 0.15);
    } catch (e) {}
}

// 更新标记者显示
function updateITIndicator() {
    const el = document.getElementById('it-indicator');
    const it = gameState.players.find(p => p.isIT);
    if (it) {
        el.textContent = 'P' + (it.id + 1) + ' 是鬼!';
        el.style.background = 'rgba(255,0,0,0.8)';
    }
}

function resetVolcanoEvent(level) {
    gameState.volcanoEvent = {
        enabled: level === 2,
        state: 'idle',
        timer: level === 2 ? (CONFIG.gameTime * 1000) / 2 : VOLCANO_EVENT_CONFIG.firstDelay,
        rocks: [],
        meltTargets: [],
        spawnTimer: 0,
        shakeTime: 0,
        nextRockSide: 1
    };
}

function startVolcanoWarning() {
    if (!gameState.volcanoEvent || !gameState.volcanoEvent.enabled) return;
    gameState.volcanoEvent.state = 'warning';
    gameState.volcanoEvent.timer = VOLCANO_EVENT_CONFIG.warningDuration;
    gameState.volcanoEvent.spawnTimer = 0;
}

function startVolcanoEruption() {
    if (!gameState.volcanoEvent || !gameState.volcanoEvent.enabled) return;
    gameState.volcanoEvent.state = 'erupting';
    gameState.volcanoEvent.timer = VOLCANO_EVENT_CONFIG.eruptionDuration;
    gameState.volcanoEvent.spawnTimer = 0;
    gameState.volcanoEvent.shakeTime = VOLCANO_EVENT_CONFIG.eruptionDuration;
    selectVolcanoMeltTargets();
    playVolcanoEruptionMusic(true);
}

function finishVolcanoEruption() {
    if (!gameState.volcanoEvent) return;
    if (gameState.volcanoEvent.meltTargets) {
        gameState.volcanoEvent.meltTargets.forEach(platform => {
            if (!platform || platform.broken) return;
            platform.broken = true;
            platform.isMelting = false;
            platform.meltProgress = 1;
        });
        gameState.volcanoEvent.meltTargets = [];
    }
    gameState.volcanoEvent.state = 'idle';
    gameState.volcanoEvent.timer = VOLCANO_EVENT_CONFIG.cooldown;
    gameState.volcanoEvent.spawnTimer = 0;
    gameState.volcanoEvent.shakeTime = 0;
    stopVolcanoEruptionMusic(true);
}

function spawnVolcanoRock() {
    const viewTop = gameState.cameraY;
    const size = VOLCANO_EVENT_CONFIG.minRockSize +
        Math.random() * (VOLCANO_EVENT_CONFIG.maxRockSize - VOLCANO_EVENT_CONFIG.minRockSize);
    const margin = Math.max(size + 12, gameState.worldWidth * 0.04);
    const x = margin + Math.random() * Math.max(1, gameState.worldWidth - margin * 2);
    const y = viewTop - size - 80 - Math.random() * 120;
    gameState.volcanoEvent.rocks.push(new VolcanoRock(x, y, size));
}

function selectVolcanoMeltTargets() {
    const event = gameState.volcanoEvent;
    if (!event || !event.enabled) return;

    const candidates = gameState.platforms
        .map((platform, index) => ({ platform, index }))
        .filter(({ platform, index }) =>
            index !== 0 &&
            platform.volcano &&
            platform.canMelt &&
            isPlatformSolid(platform) &&
            !platform.isMelting
        );

    const count = Math.min(VOLCANO_EVENT_CONFIG.meltingPlatformCount, candidates.length);
    event.meltTargets = [];

    for (let i = candidates.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [candidates[i], candidates[j]] = [candidates[j], candidates[i]];
    }

    for (let i = 0; i < count; i++) {
        const platform = candidates[i].platform;
        platform.isMelting = true;
        platform.meltProgress = 0;
        platform.meltTimer = 0;
        platform.meltDuration = VOLCANO_EVENT_CONFIG.platformMeltDuration + Math.random() * 1600;
        event.meltTargets.push(platform);
    }
}

function updateVolcanoPlatformMelting(dt) {
    const event = gameState.volcanoEvent;
    if (!event || !event.meltTargets) return;

    event.meltTargets.forEach(platform => {
        if (!platform || platform.broken) return;
        platform.meltTimer = (platform.meltTimer || 0) + dt;
        platform.meltProgress = Math.min(1, platform.meltTimer / platform.meltDuration);
        if (platform.meltProgress >= 1) {
            platform.broken = true;
            platform.isMelting = false;
            platform.meltProgress = 1;
        }
    });

    event.meltTargets = event.meltTargets.filter(platform => platform && !platform.broken);
}

function updateVolcanoEvent(dt) {
    const event = gameState.volcanoEvent;
    if (!event || !event.enabled || gameState.currentLevel !== 2) return;

    event.timer -= dt;

    if (event.state === 'idle' && event.timer <= 0) {
        startVolcanoWarning();
    } else if (event.state === 'warning' && event.timer <= 0) {
        startVolcanoEruption();
    } else if (event.state === 'erupting') {
        event.shakeTime = Math.max(0, event.timer);
        updateVolcanoPlatformMelting(dt);
        event.spawnTimer -= dt;
        while (event.spawnTimer <= 0) {
            spawnVolcanoRock();
            event.spawnTimer += VOLCANO_EVENT_CONFIG.rockSpawnInterval;
        }
        if (event.timer <= 0) {
            finishVolcanoEruption();
        }
    }

    event.rocks.forEach(rock => {
        rock.update(dt);

        gameState.players.forEach(player => {
            if (rock.checkPlayerCollision(player)) {
                rock.hitPlayers.add(player.id);
                player.zapped = true;
                player.zappedTimer = VOLCANO_EVENT_CONFIG.rockStunDuration;
                player.vx = 0;
                player.vy = 0;
            }
        });
    });

    event.rocks = event.rocks.filter(rock => rock.active);
}

function getVolcanoShakeOffset() {
    const event = gameState.volcanoEvent;
    if (!event || !event.enabled || event.state !== 'erupting') return { x: 0, y: 0 };
    const fade = Math.min(1, Math.max(0.25, event.shakeTime / VOLCANO_EVENT_CONFIG.eruptionDuration));
    const strength = VOLCANO_EVENT_CONFIG.shakeStrength * fade;
    return {
        x: (Math.random() - 0.5) * strength * 2,
        y: (Math.random() - 0.5) * strength * 1.4
    };
}

function drawVolcanoWarningOverlay(ctx) {
    const event = gameState.volcanoEvent;
    if (!event || !event.enabled || gameState.currentLevel !== 2) return;
    if (event.state !== 'warning' && event.state !== 'erupting') return;

    const canvas = ctx.canvas;
    const isWarning = event.state === 'warning';
    const seconds = Math.max(0, Math.ceil(event.timer / 1000));

    if (event.state === 'erupting') {
        ctx.save();
        ctx.fillStyle = 'rgba(255, 72, 18, 0.13)';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.restore();
    }

    ctx.save();
    const panelW = Math.min(480, canvas.width - 32);
    const panelH = isWarning ? 88 : 64;
    const x = (canvas.width - panelW) / 2;
    const y = 82;

    ctx.fillStyle = isWarning ? 'rgba(45, 8, 0, 0.84)' : 'rgba(80, 18, 0, 0.76)';
    ctx.strokeStyle = isWarning ? '#ffb347' : '#ff5a12';
    ctx.lineWidth = 2;
    const r = 14;
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.lineTo(x + panelW - r, y);
    ctx.quadraticCurveTo(x + panelW, y, x + panelW, y + r);
    ctx.lineTo(x + panelW, y + panelH - r);
    ctx.quadraticCurveTo(x + panelW, y + panelH, x + panelW - r, y + panelH);
    ctx.lineTo(x + r, y + panelH);
    ctx.quadraticCurveTo(x, y + panelH, x, y + panelH - r);
    ctx.lineTo(x, y + r);
    ctx.quadraticCurveTo(x, y, x + r, y);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();

    ctx.textAlign = 'center';
    ctx.fillStyle = '#fff4df';
    ctx.font = 'bold 22px Arial';
    ctx.fillText(isWarning ? '⚠ 火山即将爆发' : '🌋 火山爆发中', canvas.width / 2, y + 30);

    if (isWarning) {
        ctx.fillStyle = '#ffcf66';
        ctx.font = 'bold 34px monospace';
        ctx.fillText(`${seconds}`, canvas.width / 2, y + 68);
    } else {
        ctx.fillStyle = '#ffd8a8';
        ctx.font = 'bold 15px Arial';
        ctx.fillText('注意躲避滚动岩石，被击中会定身 3 秒', canvas.width / 2, y + 48);
    }
    ctx.restore();
}

function resetCityAttackEvent(level) {
    gameState.cityAttackEvent = {
        enabled: level === 1,
        state: 'idle',
        timer: CITY_ATTACK_CONFIG.firstDelay,
        elapsed: 0,
        laserTimer: 900,
        lasers: [],
        homelander: { x: -120, y: 120, targetX: 0, targetY: 120 },
        helicopter: { x: 0, y: 106 },
        explosion: null
    };
}

function startCityAttackEvent() {
    const event = gameState.cityAttackEvent;
    if (!event || !event.enabled) return;
    event.state = 'attacking';
    event.elapsed = 0;
    event.laserTimer = 650;
    event.lasers = [];
    event.homelander.x = -120;
    event.homelander.y = 120;
    event.homelander.targetX = gameState.worldWidth * 0.55;
    event.homelander.targetY = 120;
    playCityAttackMusic(true);
}

function startCityHelicopterCollision() {
    const event = gameState.cityAttackEvent;
    if (!event || !event.enabled) return;
    event.state = 'collision';
    event.elapsed = 0;
    event.lasers = [];
    event.helicopter.x = -180;
    event.helicopter.y = event.homelander.y - 14;
    playHelicopterMusic(true);
}

function startCityAirExplosion() {
    const event = gameState.cityAttackEvent;
    if (!event || !event.enabled) return;
    playHelicopterExplosionSound(true);
    event.state = 'exploding';
    event.elapsed = 0;
    event.explosion = {
        x: event.homelander.x,
        y: event.homelander.y,
        particles: Array.from({ length: 34 }, () => {
            const angle = Math.random() * Math.PI * 2;
            const speed = 2 + Math.random() * 7;
            return {
                x: 0,
                y: 0,
                vx: Math.cos(angle) * speed,
                vy: Math.sin(angle) * speed,
                size: 5 + Math.random() * 14,
                color: Math.random() > 0.45 ? '#ff7a18' : '#ffd166'
            };
        })
    };
}

function finishCityAttackEvent() {
    const event = gameState.cityAttackEvent;
    if (!event) return;
    if (audioState.specialTrack === 'helicopterExplosion') {
        stopSpecialMusic(true);
    }
    event.enabled = false;
    event.state = 'done';
    event.lasers = [];
}

function randomCityLaserDelay() {
    return CITY_ATTACK_CONFIG.laserIntervalMin +
        Math.random() * (CITY_ATTACK_CONFIG.laserIntervalMax - CITY_ATTACK_CONFIG.laserIntervalMin);
}

function fireCityLaser() {
    const event = gameState.cityAttackEvent;
    if (!event || !event.enabled || gameState.players.length === 0) return;

    const visiblePlayers = gameState.players.filter(player => {
        const sy = player.y - gameState.cameraY;
        return sy > 80 && sy < gameState.worldHeight + 60;
    });
    const targetPlayer = visiblePlayers[Math.floor(Math.random() * visiblePlayers.length)] ||
        gameState.players[Math.floor(Math.random() * gameState.players.length)];

    const targetX = targetPlayer.x + (Math.random() - 0.5) * 80;
    const targetY = targetPlayer.y - gameState.cameraY + (Math.random() - 0.5) * 30;

    event.lasers.push({
        x1: event.homelander.x - 8,
        y1: event.homelander.y - 16,
        x2: targetX,
        y2: targetY,
        age: 0,
        hitPlayers: new Set()
    });
}

function distanceToSegment(px, py, x1, y1, x2, y2) {
    const dx = x2 - x1;
    const dy = y2 - y1;
    const lenSq = dx * dx + dy * dy;
    if (lenSq === 0) return Math.hypot(px - x1, py - y1);
    const t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / lenSq));
    const sx = x1 + t * dx;
    const sy = y1 + t * dy;
    return Math.hypot(px - sx, py - sy);
}

function updateCityAttackEvent(dt) {
    const event = gameState.cityAttackEvent;
    if (!event || !event.enabled || gameState.currentLevel !== 1) return;

    if (event.state === 'idle') {
        event.timer -= dt;
        if (event.timer <= 0) startCityAttackEvent();
        return;
    }

    if (event.state === 'attacking') {
        event.elapsed += dt;
        const entry = Math.min(1, event.elapsed / 2400);
        const hover = Math.sin(event.elapsed / 460);
        event.homelander.x = -120 + (event.homelander.targetX + 120) * entry + Math.sin(event.elapsed / 760) * 62 * entry;
        event.homelander.y = event.homelander.targetY + hover * 12;

        event.laserTimer -= dt;
        if (event.laserTimer <= 0 && event.elapsed > 900) {
            fireCityLaser();
            event.laserTimer = randomCityLaserDelay();
        }

        if (event.elapsed >= CITY_ATTACK_CONFIG.homelanderDuration) {
            startCityHelicopterCollision();
        }
    } else if (event.state === 'collision') {
        event.elapsed += dt;
        const t = Math.min(1, event.elapsed / CITY_ATTACK_CONFIG.helicopterDuration);
        const startX = -180;
        const endX = event.homelander.x - 75;
        event.helicopter.x = startX + (endX - startX) * t;
        event.helicopter.y = event.homelander.y - 10 + Math.sin(event.elapsed / 90) * 4;
        if (t >= 1) startCityAirExplosion();
    } else if (event.state === 'exploding') {
        event.elapsed += dt;
        if (event.explosion) {
            event.explosion.particles.forEach(particle => {
                particle.x += particle.vx;
                particle.y += particle.vy;
                particle.vx *= 0.96;
                particle.vy = particle.vy * 0.96 + 0.05;
            });
        }
        if (event.elapsed >= CITY_ATTACK_CONFIG.explosionDuration) {
            finishCityAttackEvent();
        }
    }

    event.lasers.forEach(laser => {
        laser.age += dt;
        gameState.players.forEach(player => {
            if (laser.hitPlayers.has(player.id)) return;
            const playerScreenY = player.y - gameState.cameraY;
            const distance = distanceToSegment(player.x, playerScreenY, laser.x1, laser.y1, laser.x2, laser.y2);
            if (distance < player.size * 0.82) {
                laser.hitPlayers.add(player.id);
                player.zapped = true;
                player.zappedTimer = CITY_ATTACK_CONFIG.laserStunDuration;
                player.vx = 0;
                player.vy = 0;
            }
        });
    });
    event.lasers = event.lasers.filter(laser => laser.age <= CITY_ATTACK_CONFIG.laserDuration);
}

function drawHomelanderFigure(ctx, x, y, scale = 1) {
    const asset = CITY_ATTACK_IMAGES.homelander;
    if (asset.loaded && asset.image) {
        const img = asset.image;
        let sx = 0;
        let sy = 0;
        let sw = img.naturalWidth || img.width;
        let sh = img.naturalHeight || img.height;

        if (sw > sh * 1.12) {
            sw *= 0.48;
            sh *= 0.58;
        }

        ctx.save();
        ctx.translate(x, y);
        ctx.scale(scale, scale);
        ctx.shadowColor = 'rgba(255, 40, 40, 0.55)';
        ctx.shadowBlur = 18;
        ctx.drawImage(img, sx, sy, sw, sh, -46, -94, 92, 172);
        ctx.restore();
        return;
    }

    ctx.save();
    ctx.translate(x, y);
    ctx.scale(scale, scale);

    ctx.fillStyle = 'rgba(170, 0, 0, 0.88)';
    ctx.beginPath();
    ctx.moveTo(-17, -10);
    ctx.lineTo(-38, 44);
    ctx.lineTo(35, 42);
    ctx.lineTo(16, -10);
    ctx.closePath();
    ctx.fill();

    ctx.fillStyle = '#173b86';
    ctx.fillRect(-13, -12, 26, 34);
    ctx.fillStyle = '#c79a32';
    ctx.fillRect(-20, -11, 40, 7);
    ctx.fillStyle = '#d6a552';
    ctx.beginPath();
    ctx.arc(0, -27, 12, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = '#f2cc72';
    ctx.fillRect(-10, -39, 20, 11);

    ctx.fillStyle = '#b61d22';
    ctx.fillRect(-24, 18, 12, 24);
    ctx.fillRect(12, 18, 12, 24);
    ctx.fillStyle = '#fff';
    ctx.font = 'bold 16px Arial';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('祖', 0, 6);

    ctx.restore();
}

function drawKobeHelicopter(ctx, x, y, scale = 1) {
    const asset = CITY_ATTACK_IMAGES.kobeHelicopter;
    if (asset.loaded && asset.image) {
        const img = asset.image;
        ctx.save();
        ctx.translate(x, y);
        ctx.scale(scale, scale);
        ctx.shadowColor = 'rgba(255, 190, 70, 0.45)';
        ctx.shadowBlur = 18;
        ctx.drawImage(img, -110, -78, 220, 156);

        const rotorSpin = Date.now() / 20;
        ctx.strokeStyle = 'rgba(255,255,255,0.72)';
        ctx.lineWidth = 3;
        ctx.save();
        ctx.rotate(Math.sin(rotorSpin) * 0.07);
        ctx.beginPath();
        ctx.moveTo(-94, -76);
        ctx.lineTo(94, -76);
        ctx.stroke();
        ctx.restore();
        ctx.restore();
        return;
    }

    ctx.save();
    ctx.translate(x, y);
    ctx.scale(scale, scale);

    const rotorSpin = Date.now() / 22;
    ctx.strokeStyle = 'rgba(230,230,230,0.8)';
    ctx.lineWidth = 3;
    ctx.save();
    ctx.rotate(Math.sin(rotorSpin) * 0.08);
    ctx.beginPath();
    ctx.moveTo(-70, -26);
    ctx.lineTo(70, -26);
    ctx.moveTo(0, -42);
    ctx.lineTo(0, -10);
    ctx.stroke();
    ctx.restore();

    ctx.fillStyle = '#16191f';
    ctx.beginPath();
    ctx.ellipse(0, 0, 52, 22, 0, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = '#f5b62d';
    ctx.fillRect(-38, -9, 58, 18);
    ctx.fillStyle = '#552583';
    ctx.font = 'bold 15px Arial';
    ctx.textAlign = 'center';
    ctx.fillText('KOBE 24', -6, 5);

    ctx.strokeStyle = '#111';
    ctx.lineWidth = 4;
    ctx.beginPath();
    ctx.moveTo(48, -4);
    ctx.lineTo(88, -18);
    ctx.lineTo(104, -13);
    ctx.stroke();
    ctx.strokeStyle = '#222';
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.moveTo(-34, 24);
    ctx.lineTo(34, 24);
    ctx.moveTo(-24, 15);
    ctx.lineTo(-32, 24);
    ctx.moveTo(24, 15);
    ctx.lineTo(32, 24);
    ctx.stroke();

    ctx.restore();
}

function drawCityLaser(ctx, laser) {
    const alpha = Math.max(0, 1 - laser.age / CITY_ATTACK_CONFIG.laserDuration);
    ctx.save();
    ctx.globalAlpha = alpha;
    ctx.strokeStyle = '#ff1f1f';
    ctx.shadowColor = '#ff0000';
    ctx.shadowBlur = 22;
    ctx.lineWidth = 7;
    ctx.beginPath();
    ctx.moveTo(laser.x1, laser.y1);
    ctx.lineTo(laser.x2, laser.y2);
    ctx.stroke();
    ctx.strokeStyle = '#ffd0d0';
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.restore();
}

function drawCityAirExplosion(ctx, explosion, elapsed) {
    if (!explosion) return;
    const alpha = Math.max(0, 1 - elapsed / CITY_ATTACK_CONFIG.explosionDuration);
    ctx.save();
    ctx.translate(explosion.x, explosion.y);
    ctx.globalAlpha = alpha;
    const radius = 42 + elapsed * 0.08;
    const gradient = ctx.createRadialGradient(0, 0, 0, 0, 0, radius);
    gradient.addColorStop(0, '#fff2a8');
    gradient.addColorStop(0.35, '#ff8a1c');
    gradient.addColorStop(1, 'rgba(180, 0, 0, 0)');
    ctx.fillStyle = gradient;
    ctx.beginPath();
    ctx.arc(0, 0, radius, 0, Math.PI * 2);
    ctx.fill();

    explosion.particles.forEach(particle => {
        ctx.fillStyle = particle.color;
        ctx.beginPath();
        ctx.arc(particle.x, particle.y, particle.size * alpha, 0, Math.PI * 2);
        ctx.fill();
    });
    ctx.restore();
}

function drawCityAttackOverlay(ctx) {
    const event = gameState.cityAttackEvent;
    if (!event || !event.enabled || gameState.currentLevel !== 1) return;
    if (event.state === 'idle' || event.state === 'done') return;

    event.lasers.forEach(laser => drawCityLaser(ctx, laser));

    if (event.state === 'attacking' || event.state === 'collision') {
        drawHomelanderFigure(ctx, event.homelander.x, event.homelander.y, 1);
        if (event.state === 'collision') {
            drawKobeHelicopter(ctx, event.helicopter.x, event.helicopter.y, 1);
        }
    }

    if (event.state === 'exploding') {
        drawCityAirExplosion(ctx, event.explosion, event.elapsed);
    }

    if (event.state === 'attacking' && event.elapsed < 2200) {
        ctx.save();
        ctx.textAlign = 'center';
        ctx.fillStyle = 'rgba(0,0,0,0.62)';
        ctx.fillRect(ctx.canvas.width / 2 - 160, 118, 320, 44);
        ctx.strokeStyle = '#ff4d4d';
        ctx.strokeRect(ctx.canvas.width / 2 - 160, 118, 320, 44);
        ctx.fillStyle = '#fff1d0';
        ctx.font = 'bold 22px Arial';
        ctx.fillText('祖国人来袭', ctx.canvas.width / 2, 147);
        ctx.restore();
    }
}

function drawFallbackLevelBackground(ctx, level, width, height) {
    const gradient = ctx.createLinearGradient(0, 0, 0, height);

    if (level === 2) {
        gradient.addColorStop(0, '#211018');
        gradient.addColorStop(0.55, '#4a1b12');
        gradient.addColorStop(1, '#15070a');
        ctx.fillStyle = gradient;
        ctx.fillRect(0, 0, width, height);

        ctx.fillStyle = 'rgba(255, 83, 18, 0.22)';
        ctx.beginPath();
        ctx.arc(width * 0.5, height * 0.14, Math.min(width, height) * 0.22, 0, Math.PI * 2);
        ctx.fill();

        ctx.fillStyle = 'rgba(255, 130, 30, 0.24)';
        for (let i = 0; i < 7; i++) {
            const x = width * (0.12 + i * 0.13);
            ctx.beginPath();
            ctx.moveTo(x, height);
            ctx.lineTo(x + width * 0.05, height * (0.38 + (i % 3) * 0.08));
            ctx.lineTo(x + width * 0.12, height);
            ctx.closePath();
            ctx.fill();
        }
        return;
    }

    if (level === 3) {
        gradient.addColorStop(0, '#47644b');
        gradient.addColorStop(0.55, '#243d2f');
        gradient.addColorStop(1, '#111f18');
        ctx.fillStyle = gradient;
        ctx.fillRect(0, 0, width, height);

        ctx.fillStyle = 'rgba(19, 48, 30, 0.42)';
        for (let i = 0; i < 9; i++) {
            const x = width * (i / 8);
            ctx.fillRect(x - 18, height * 0.15, 36, height);
        }
        return;
    }

    gradient.addColorStop(0, '#8ed5ff');
    gradient.addColorStop(0.5, '#3b7da7');
    gradient.addColorStop(1, '#1b2740');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, width, height);

    ctx.fillStyle = 'rgba(20, 34, 58, 0.58)';
    for (let i = 0; i < 12; i++) {
        const buildingW = width / 16;
        const x = i * buildingW * 1.4;
        const buildingH = height * (0.22 + (i % 5) * 0.07);
        ctx.fillRect(x, height - buildingH, buildingW, buildingH);
    }
}

// 渲染
function render() {
    const canvas = document.getElementById('game-canvas');
    const ctx = canvas.getContext('2d');
    const offsetY = gameState.cameraY;
    const shake = getVolcanoShakeOffset();

    ctx.save();
    ctx.translate(shake.x, shake.y);

    // 绘制背景；图片失败时使用对应地图的备用背景，避免黑屏。
    if (gameState.backgroundImage && gameState.backgroundLoaded) {
        ctx.drawImage(gameState.backgroundImage, 0, 0, canvas.width, canvas.height);
    } else {
        drawFallbackLevelBackground(ctx, gameState.currentLevel, canvas.width, canvas.height);
    }

    gameState.platforms.forEach(p => p.draw(ctx, offsetY));
    gameState.springs.forEach(s => s.draw(ctx, offsetY));
    gameState.traps.forEach(t => t.draw(ctx, offsetY));
    gameState.items.forEach(item => item.draw(ctx, offsetY));
    if (gameState.volcanoEvent && gameState.volcanoEvent.rocks) {
        gameState.volcanoEvent.rocks.forEach(rock => rock.draw(ctx, offsetY));
    }

    // 绘制闪电效果
    gameState.zapEffects.forEach(zap => zap.draw(ctx, offsetY));

    // 绘制专属能量球
    gameState.powerOrbs.forEach(orb => orb.draw(ctx, offsetY));

    // 绘制能量球拾取特效
    drawPowerOrbEffects(ctx, offsetY);

    // 绘制错误拾取特效
    drawOrbFailEffects(ctx, offsetY);

    // 绘制全局道具使用特效
    drawAllItemEffects(ctx, offsetY);

    gameState.players.forEach(p => {
        drawItemEffects(ctx, p, offsetY);
        drawPlayer(ctx, p, offsetY);
    });

    // 绘制标记特效
    drawTagEffects(ctx, offsetY);

    ctx.restore();
    drawVolcanoWarningOverlay(ctx);
    drawCityAttackOverlay(ctx);
}

// 游戏循环
function gameLoop() {
    if (!gameState.running) return;

    // 计算delta time
    const now = Date.now();
    const dt = gameState.lastTime ? now - gameState.lastTime : 16.67;
    gameState.lastTime = now;

    const platformTime = Date.now();
    gameState.platforms.forEach(platform => platform.update(platformTime));

    updateCamera();  // 更新相机
    gameState.players.forEach((p, i) => updatePlayer(p, i, dt));
    updateVolcanoEvent(dt);
    updateCityAttackEvent(dt);
    checkCollision();
    render();

    // 更新闪电效果
    gameState.zapEffects.forEach(zap => zap.update());
    gameState.zapEffects = gameState.zapEffects.filter(zap => !zap.isExpired());

    // 计算游戏已进行时间（秒）
    const elapsedTime = CONFIG.gameTime - gameState.timeLeft;

    // 道具刷新（频率从低到高：开始15秒一次，逐渐加快到5秒一次）
    gameState.itemSpawnTimer++;
    const itemInterval = Math.max(300, 900 - elapsedTime * 10); // 从900帧降到最低300帧
    if (gameState.itemSpawnTimer >= itemInterval) {
        if (gameState.items.length < 5) { // 最多5个道具（后期增加）
            spawnItem();
        }
        gameState.itemSpawnTimer = 0;
    }

    // 补鼠夹刷新（从20秒逐渐加快到8秒）
    gameState.trapSpawnTimer++;
    const trapInterval = Math.max(480, 1200 - elapsedTime * 15); // 从1200帧降到最低480帧
    if (gameState.trapSpawnTimer >= trapInterval) {
        if (gameState.traps.length < 4) { // 最多4个补鼠夹（后期增加）
            const platforms = getSpawnablePlatforms();
            const plat = platforms[Math.floor(Math.random() * platforms.length)];
            const trapX = plat.x + Math.random() * (plat.width - 30);
            const trapY = plat.y - 15;
            gameState.traps.push(new Trap(trapX, trapY));
        }
        gameState.trapSpawnTimer = 0;
    }

    // 专属能量球刷新（从30秒逐渐加快到15秒）
    gameState.powerOrbSpawnTimer++;
    const orbInterval = Math.max(900, 1800 - elapsedTime * 20); // 从1800帧降到最低900帧
    if (gameState.powerOrbSpawnTimer >= orbInterval) {
        if (gameState.powerOrbs.length < 3) {
            const targetPlayerId = Math.floor(Math.random() * gameState.players.length);
            gameState.powerOrbs.push(new PowerOrb(targetPlayerId));
        }
        gameState.powerOrbSpawnTimer = 0;
    }

    // 更新能量球
    gameState.powerOrbs.forEach(orb => orb.update(dt));
    gameState.powerOrbs = gameState.powerOrbs.filter(orb => orb.active);

    requestAnimationFrame(gameLoop);
}

// 更新相机（跟随玩家，但不能超出世界边界）
function updateCamera() {
    const canvas = document.getElementById('game-canvas');
    const viewHeight = canvas.height;

    // 找到最高和最低的玩家
    let minY = Infinity;
    let maxY = -Infinity;
    gameState.players.forEach(p => {
        minY = Math.min(minY, p.y);
        maxY = Math.max(maxY, p.y);
    });

    // 计算相机应该显示的中心点
    const centerY = (minY + maxY) / 2;

    // 计算相机的目标Y偏移（让中心点在画布中央）
    const targetOffsetY = centerY - viewHeight / 2;

    // 限制相机不能超出世界
    const maxOffsetY = gameState.worldHeight - viewHeight;
    const minOffsetY = 0;

    // 平滑移动相机
    gameState.cameraY += (Math.max(minOffsetY, Math.min(maxOffsetY, targetOffsetY)) - gameState.cameraY) * 0.1;
}

function loadLevelBackground(path) {
    if (gameState.backgroundPath === path && gameState.backgroundLoaded) return;

    gameState.backgroundPath = path;
    gameState.backgroundLoaded = false;
    gameState.backgroundImage = new Image();
    gameState.backgroundImage.onload = () => {
        gameState.backgroundLoaded = true;
        console.log('背景图片加载成功');
    };
    gameState.backgroundImage.onerror = () => {
        console.log('背景图片加载失败，使用纯色背景');
    };
    gameState.backgroundImage.src = path;
}

function updateMenuMapBackground(mapId = CONFIG.selectedMap) {
    const menu = document.getElementById('menu');
    const path = MAP_BACKGROUNDS[mapId] || MAP_BACKGROUNDS[1];
    if (!menu) return;
    const resolvedPath = new URL(path, window.location.href).href;
    menu.style.setProperty('--menu-map-bg', `url("${resolvedPath}")`);
}

function preloadMenuMapBackgrounds() {
    Object.values(MAP_BACKGROUNDS).forEach(path => {
        const img = new Image();
        img.src = path;
    });
}

function updateMusicButton() {
    const button = document.getElementById('music-toggle');
    if (!button) return;

    if (audioState.missing) {
        button.textContent = '无音乐';
        button.classList.add('muted');
        return;
    }

    button.textContent = audioState.enabled ? '音乐开' : '音乐关';
    button.classList.toggle('muted', !audioState.enabled);
}

function isScreenActive(id) {
    const screen = document.getElementById(id);
    return !!screen && screen.classList.contains('active');
}

function initBackgroundMusic() {
    if (typeof Audio === 'undefined') return;

    const loadCandidate = index => {
        if (index >= AUDIO_CONFIG.bgmCandidates.length) {
            audioState.available = false;
            audioState.missing = true;
            audioState.bgm = null;
            updateMusicButton();
            return;
        }

        const audio = new Audio(AUDIO_CONFIG.bgmCandidates[index]);
        audio.loop = true;
        audio.volume = AUDIO_CONFIG.bgmVolume;
        audio.preload = 'auto';

        audio.addEventListener('canplaythrough', () => {
            audioState.available = true;
            audioState.missing = false;
            updateMusicButton();
            if ((gameState.running || isScreenActive('menu')) && !audioState.specialActive) {
                playBackgroundMusic();
            }
        }, { once: true });

        audio.addEventListener('error', () => {
            if (audioState.bgm !== audio) return;
            audioState.candidateIndex = index + 1;
            loadCandidate(audioState.candidateIndex);
        }, { once: true });

        audioState.bgm = audio;
    };

    const loadHomelanderCandidate = index => {
        if (index >= AUDIO_CONFIG.homelanderCandidates.length) {
            audioState.homelanderAvailable = false;
            audioState.homelanderMissing = true;
            audioState.homelander = null;
            return;
        }

        const audio = new Audio(AUDIO_CONFIG.homelanderCandidates[index]);
        audio.loop = true;
        audio.volume = AUDIO_CONFIG.homelanderVolume;
        audio.preload = 'auto';

        audio.addEventListener('canplaythrough', () => {
            audioState.homelanderAvailable = true;
            audioState.homelanderMissing = false;
        }, { once: true });

        audio.addEventListener('error', () => {
            if (audioState.homelander !== audio) return;
            audioState.homelanderCandidateIndex = index + 1;
            loadHomelanderCandidate(audioState.homelanderCandidateIndex);
        }, { once: true });

        audioState.homelander = audio;
    };

    const loadHelicopterCandidate = index => {
        if (index >= AUDIO_CONFIG.helicopterCandidates.length) {
            audioState.helicopterAvailable = false;
            audioState.helicopterMissing = true;
            audioState.helicopter = null;
            return;
        }

        const audio = new Audio(AUDIO_CONFIG.helicopterCandidates[index]);
        audio.loop = true;
        audio.volume = AUDIO_CONFIG.helicopterVolume;
        audio.preload = 'auto';

        audio.addEventListener('canplaythrough', () => {
            audioState.helicopterAvailable = true;
            audioState.helicopterMissing = false;
        }, { once: true });

        audio.addEventListener('error', () => {
            if (audioState.helicopter !== audio) return;
            audioState.helicopterCandidateIndex = index + 1;
            loadHelicopterCandidate(audioState.helicopterCandidateIndex);
        }, { once: true });

        audioState.helicopter = audio;
    };

    const loadHelicopterExplosionCandidate = index => {
        if (index >= AUDIO_CONFIG.helicopterExplosionCandidates.length) {
            audioState.helicopterExplosionAvailable = false;
            audioState.helicopterExplosionMissing = true;
            audioState.helicopterExplosion = null;
            return;
        }

        const audio = new Audio(AUDIO_CONFIG.helicopterExplosionCandidates[index]);
        audio.loop = false;
        audio.volume = AUDIO_CONFIG.helicopterExplosionVolume;
        audio.preload = 'auto';

        audio.addEventListener('canplaythrough', () => {
            audioState.helicopterExplosionAvailable = true;
            audioState.helicopterExplosionMissing = false;
        }, { once: true });

        audio.addEventListener('ended', () => {
            if (audioState.specialTrack === 'helicopterExplosion') {
                stopSpecialMusic(true);
            }
        });

        audio.addEventListener('error', () => {
            if (audioState.helicopterExplosion !== audio) return;
            audioState.helicopterExplosionCandidateIndex = index + 1;
            loadHelicopterExplosionCandidate(audioState.helicopterExplosionCandidateIndex);
        }, { once: true });

        audioState.helicopterExplosion = audio;
    };

    const loadVolcanoEruptionCandidate = index => {
        if (index >= AUDIO_CONFIG.volcanoEruptionCandidates.length) {
            audioState.volcanoEruptionAvailable = false;
            audioState.volcanoEruptionMissing = true;
            audioState.volcanoEruption = null;
            return;
        }

        const audio = new Audio(AUDIO_CONFIG.volcanoEruptionCandidates[index]);
        audio.loop = true;
        audio.volume = AUDIO_CONFIG.volcanoEruptionVolume;
        audio.preload = 'auto';

        audio.addEventListener('canplaythrough', () => {
            audioState.volcanoEruptionAvailable = true;
            audioState.volcanoEruptionMissing = false;
        }, { once: true });

        audio.addEventListener('error', () => {
            if (audioState.volcanoEruption !== audio) return;
            audioState.volcanoEruptionCandidateIndex = index + 1;
            loadVolcanoEruptionCandidate(audioState.volcanoEruptionCandidateIndex);
        }, { once: true });

        audioState.volcanoEruption = audio;
    };

    loadCandidate(0);
    loadHomelanderCandidate(0);
    loadHelicopterCandidate(0);
    loadHelicopterExplosionCandidate(0);
    loadVolcanoEruptionCandidate(0);
    updateMusicButton();
}

function playBackgroundMusic(restart = false) {
    if (!audioState.enabled || !audioState.bgm) return;
    if (audioState.specialActive) return;

    if (restart) {
        try {
            audioState.bgm.currentTime = 0;
        } catch (error) {
            // Some browsers may reject seeking before metadata is loaded.
        }
    }

    const playPromise = audioState.bgm.play();
    if (playPromise && typeof playPromise.catch === 'function') {
        playPromise.catch(() => {
            // The game can continue silently if the browser blocks playback or no file exists.
        });
    }
}

function pauseBackgroundMusic(reset = false) {
    if (!audioState.bgm) return;

    audioState.bgm.pause();
    if (reset) {
        try {
            audioState.bgm.currentTime = 0;
        } catch (error) {
            // Ignore seek failures for partially loaded audio.
        }
    }
}

function playCityAttackMusic(restart = true) {
    audioState.specialActive = true;
    audioState.specialTrack = 'homelander';
    pauseBackgroundMusic(false);

    if (!audioState.enabled || !audioState.homelander) return;

    if (restart) {
        try {
            audioState.homelander.currentTime = 0;
        } catch (error) {
            // Ignore seek failures before metadata is ready.
        }
    }

    const playPromise = audioState.homelander.play();
    if (playPromise && typeof playPromise.catch === 'function') {
        playPromise.catch(() => {
            // Silent fallback if the browser blocks playback.
        });
    }
}

function pauseCityAttackMusic(reset = false) {
    if (!audioState.homelander) return;

    audioState.homelander.pause();
    if (reset) {
        try {
            audioState.homelander.currentTime = 0;
        } catch (error) {
            // Ignore seek failures for partially loaded audio.
        }
    }
}

function playHelicopterMusic(restart = true) {
    audioState.specialActive = true;
    audioState.specialTrack = 'helicopter';
    pauseBackgroundMusic(false);
    pauseCityAttackMusic(true);

    if (!audioState.enabled || !audioState.helicopter) return;

    if (restart) {
        try {
            audioState.helicopter.currentTime = 0;
        } catch (error) {
            // Ignore seek failures before metadata is ready.
        }
    }

    const playPromise = audioState.helicopter.play();
    if (playPromise && typeof playPromise.catch === 'function') {
        playPromise.catch(() => {
            // Silent fallback if the browser blocks playback.
        });
    }
}

function pauseHelicopterMusic(reset = false) {
    if (!audioState.helicopter) return;

    audioState.helicopter.pause();
    if (reset) {
        try {
            audioState.helicopter.currentTime = 0;
        } catch (error) {
            // Ignore seek failures for partially loaded audio.
        }
    }
}

function playHelicopterExplosionSound(restart = true) {
    audioState.specialActive = true;
    audioState.specialTrack = 'helicopterExplosion';
    pauseBackgroundMusic(false);
    pauseCityAttackMusic(true);
    pauseHelicopterMusic(true);
    pauseVolcanoEruptionMusic(true);

    if (!audioState.enabled || !audioState.helicopterExplosion) return;

    if (restart) {
        try {
            audioState.helicopterExplosion.currentTime = 0;
        } catch (error) {
            // Ignore seek failures before metadata is ready.
        }
    }

    const playPromise = audioState.helicopterExplosion.play();
    if (playPromise && typeof playPromise.catch === 'function') {
        playPromise.catch(() => {
            // Silent fallback if the browser blocks playback.
        });
    }
}

function pauseHelicopterExplosionSound(reset = false) {
    if (!audioState.helicopterExplosion) return;

    audioState.helicopterExplosion.pause();
    if (reset) {
        try {
            audioState.helicopterExplosion.currentTime = 0;
        } catch (error) {
            // Ignore seek failures for partially loaded audio.
        }
    }
}

function playVolcanoEruptionMusic(restart = true) {
    audioState.specialActive = true;
    audioState.specialTrack = 'volcano';
    pauseBackgroundMusic(false);
    pauseCityAttackMusic(true);
    pauseHelicopterMusic(true);
    pauseHelicopterExplosionSound(true);

    if (!audioState.enabled || !audioState.volcanoEruption) return;

    if (restart) {
        try {
            audioState.volcanoEruption.currentTime = 0;
        } catch (error) {
            // Ignore seek failures before metadata is ready.
        }
    }

    const playPromise = audioState.volcanoEruption.play();
    if (playPromise && typeof playPromise.catch === 'function') {
        playPromise.catch(() => {
            // Silent fallback if the browser blocks playback.
        });
    }
}

function pauseVolcanoEruptionMusic(reset = false) {
    if (!audioState.volcanoEruption) return;

    audioState.volcanoEruption.pause();
    if (reset) {
        try {
            audioState.volcanoEruption.currentTime = 0;
        } catch (error) {
            // Ignore seek failures for partially loaded audio.
        }
    }
}

function stopVolcanoEruptionMusic(resumeBackground = true) {
    pauseVolcanoEruptionMusic(true);
    if (audioState.specialTrack === 'volcano') {
        audioState.specialActive = false;
        audioState.specialTrack = null;
        if (resumeBackground && audioState.enabled && gameState.running) {
            playBackgroundMusic(false);
        }
    }
}

function stopSpecialMusic(resumeBackground = true) {
    pauseCityAttackMusic(true);
    pauseHelicopterMusic(true);
    pauseHelicopterExplosionSound(true);
    pauseVolcanoEruptionMusic(true);
    audioState.specialActive = false;
    audioState.specialTrack = null;

    if (resumeBackground && audioState.enabled && gameState.running) {
        playBackgroundMusic(false);
    }
}

function stopCityAttackMusic(resumeBackground = true) {
    stopSpecialMusic(resumeBackground);
}

function toggleBackgroundMusic() {
    if (audioState.missing) return;

    audioState.enabled = !audioState.enabled;
    updateMusicButton();

    if (!audioState.enabled) {
        pauseBackgroundMusic();
        pauseCityAttackMusic();
        pauseHelicopterMusic();
        pauseHelicopterExplosionSound();
        pauseVolcanoEruptionMusic();
        return;
    }

    if (audioState.specialActive) {
        if (audioState.specialTrack === 'helicopter') {
            playHelicopterMusic(false);
        } else if (audioState.specialTrack === 'helicopterExplosion') {
            playHelicopterExplosionSound(false);
        } else if (audioState.specialTrack === 'volcano') {
            playVolcanoEruptionMusic(false);
        } else {
            playCityAttackMusic(false);
        }
    } else if (gameState.running || isScreenActive('menu')) {
        playBackgroundMusic();
    } else {
        pauseBackgroundMusic();
    }
}

// 开始游戏
function startGame(level = 1) {
    const canvas = document.getElementById('game-canvas');

    // 全屏尺寸
    const screenWidth = window.innerWidth;
    const screenHeight = window.innerHeight;

    gameState.worldWidth = screenWidth;
    gameState.worldHeight = screenHeight;
    canvas.width = screenWidth;
    canvas.height = screenHeight;
    gameState.currentLevel = level;
    resetVolcanoEvent(level);
    resetCityAttackEvent(level);

    loadLevelBackground(MAP_BACKGROUNDS[level] || MAP_BACKGROUNDS[1]);

    createPlatforms();

    gameState.players = [];
    for (let i = 0; i < CONFIG.playerCount; i++) {
        gameState.players.push(createPlayer(i));
    }

    const itIndex = Math.floor(Math.random() * CONFIG.playerCount);
    gameState.players[itIndex].isIT = true;

    gameState.timeLeft = CONFIG.gameTime;
    updateTimer();
    showScreen('game');

    if (gameState.timerID) clearInterval(gameState.timerID);
    gameState.timerID = setInterval(() => {
        gameState.timeLeft--;
        updateTimer();
        if (gameState.timeLeft <= 0) endGame();
    }, 1000);

    gameState.running = true;
    gameState.cameraY = 0;  // 重置相机
    gameState.items = [];   // 清空道具
    gameState.itemSpawnTimer = 0;
    gameState.powerOrbs = [];   // 清空能量球
    gameState.powerOrbSpawnTimer = 0;  // 重置能量球计时器
    gameState.powerOrbBoost = false;  // 重置加成状态
    itemEffects = [];  // 清空道具特效
    stopCityAttackMusic(false);
    playBackgroundMusic(true);
    gameLoop();
}

function startVolcanoRematch() {
    const tunnel = document.getElementById('tunnel-transition');
    tunnel.classList.add('active');
    showScreen('game');
    setTimeout(() => {
        tunnel.classList.remove('active');
        startGame(2);
    }, 1800);
}

// 更新计时器
function updateTimer() {
    const m = Math.floor(gameState.timeLeft / 60);
    const s = gameState.timeLeft % 60;
    document.getElementById('timer').textContent =
        String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
}

// 结束游戏
function endGame() {
    gameState.running = false;
    clearInterval(gameState.timerID);
    stopCityAttackMusic(false);
    pauseBackgroundMusic(true);
    const loser = gameState.players.find(p => p.isIT);
    showScreen('end-screen');
    document.getElementById('winner-text').textContent =
        gameState.currentLevel === 2 ? '🌋 火山关卡结束!' :
        gameState.currentLevel === 3 ? '🌲 森林迷宫结束!' :
        '🎉 游戏结束!';
    document.getElementById('winner-player').textContent = 'P' + (loser.id + 1) + ' 输了!';
    document.getElementById('rematch-panel').classList.remove('active');
    document.getElementById('btn-rematch-request').style.display = (gameState.currentLevel === 2 || gameState.currentLevel === 3) ? 'none' : 'inline-block';
}

// 显示画面
function showScreen(id) {
    document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
    document.getElementById(id).classList.add('active');

    if (id === 'menu') {
        stopCityAttackMusic(false);
        playBackgroundMusic(false);
    } else if (id === 'end-screen') {
        pauseBackgroundMusic(true);
    }
}

// 初始化设置
function initSettings() {
    document.querySelectorAll('.btn-option').forEach(btn => {
        btn.addEventListener('click', () => {
            const setting = btn.dataset.setting;
            const value = parseInt(btn.dataset.value);
            btn.parentElement.querySelectorAll('.btn-option')
                .forEach(b => b.classList.remove('selected'));
            btn.classList.add('selected');
            if (setting === 'players') CONFIG.playerCount = value;
            else if (setting === 'time') CONFIG.gameTime = value;
            else if (setting === 'map') {
                CONFIG.selectedMap = value;
                updateMenuMapBackground(value);
            }
        });
    });
}

// 主初始化
function init() {
    initSettings();
    preloadMenuMapBackgrounds();
    updateMenuMapBackground(CONFIG.selectedMap);
    loadPokemonSprites(); // 加载宝可梦角色图片
    loadVolcanoBarSprites(); // 加载火山横杠素材
    loadCityAttackSprites(); // 加载城市特殊事件素材
    initBackgroundMusic();
    playBackgroundMusic(false);

    document.addEventListener('keydown', e => {
        keys[e.key] = true;
        if (e.key === ' ') e.preventDefault();
    });

    document.addEventListener('keyup', e => {
        keys[e.key] = false;
    });

    const unlockMenuMusic = () => {
        if (!gameState.running && isScreenActive('menu')) {
            playBackgroundMusic(false);
        }
    };
    document.addEventListener('pointerdown', unlockMenuMusic, { once: true });
    document.addEventListener('keydown', unlockMenuMusic, { once: true });

    document.getElementById('btn-start').addEventListener('click', () => startGame(CONFIG.selectedMap));
    document.getElementById('music-toggle').addEventListener('click', toggleBackgroundMusic);
    document.getElementById('btn-restart').addEventListener('click', () => {
        showScreen('menu');
        setTimeout(() => startGame(CONFIG.selectedMap), 200);
    });
    document.getElementById('btn-rematch-request').addEventListener('click', () => {
        document.getElementById('rematch-panel').classList.add('active');
    });
    document.getElementById('btn-rematch-accept').addEventListener('click', startVolcanoRematch);
    document.getElementById('btn-menu').addEventListener('click', () => {
        gameState.running = false;
        if (gameState.timerID) clearInterval(gameState.timerID);
        showScreen('menu');
    });
}

window.addEventListener('DOMContentLoaded', init);
