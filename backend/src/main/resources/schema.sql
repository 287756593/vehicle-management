-- 企业公务车辆管理系统数据库设计
-- 数据库名: vehicle_management

CREATE DATABASE IF NOT EXISTS vehicle_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE vehicle_management;

-- ===================== 用户与权限管理 =====================
-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    dept_id BIGINT COMMENT '所属部门ID',
    role VARCHAR(20) NOT NULL COMMENT '角色: SUPER_ADMIN-超级管理员, OFFICE_ADMIN-综合办公室, DEPT_APPROVER-部门审批人, DRIVER-驾驶人, FINANCE-财务人员',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_role (role)
) COMMENT '用户表';

-- 部门表
CREATE TABLE IF NOT EXISTS sys_dept (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dept_name VARCHAR(100) NOT NULL COMMENT '部门名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父部门ID',
    dept_code VARCHAR(50) COMMENT '部门编码',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_parent (parent_id)
) COMMENT '部门表';

-- ===================== 基础数据配置 =====================
-- 车辆档案表
CREATE TABLE IF NOT EXISTS vehicle (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plate_number VARCHAR(20) NOT NULL UNIQUE COMMENT '车牌号',
    vehicle_type VARCHAR(50) COMMENT '车型',
    brand VARCHAR(50) COMMENT '品牌',
    model VARCHAR(50) COMMENT '型号',
    color VARCHAR(20) COMMENT '颜色',
    vin VARCHAR(50) COMMENT '车架号',
    engine_number VARCHAR(50) COMMENT '发动机号',
    register_date DATE COMMENT '注册日期',
    current_mileage DECIMAL(10,2) DEFAULT 0 COMMENT '当前里程(公里)',
    annual_fuel_budget DECIMAL(10,2) DEFAULT 12000 COMMENT '年燃油预算(元)',
    annual_fuel_used DECIMAL(10,2) DEFAULT 0 COMMENT '年燃油已用(元)',
    fuel_reminder_status VARCHAR(20) DEFAULT 'NONE' COMMENT '补油提醒状态: NONE-无, PENDING-待补油, COMPLETED-已完成',
    fuel_reminder_note VARCHAR(255) COMMENT '补油提醒说明',
    fuel_reminder_time DATETIME COMMENT '补油提醒时间',
    status VARCHAR(20) DEFAULT 'NORMAL' COMMENT '车辆状态: NORMAL-正常, IN_USE-使用中, PENDING_CHECK-待复核, MAINTENANCE-维修中, SCRAP-报废',
    parking_location VARCHAR(100) DEFAULT '西京酒店停车场' COMMENT '停放位置',
    insurance_company VARCHAR(100) COMMENT '保险公司',
    insurance_expire_date DATE COMMENT '保险到期日期',
    inspection_expire_date DATE COMMENT '年检到期日期',
    driving_license_photo VARCHAR(255) COMMENT '行驶证照片路径',
    insurance_photo VARCHAR(255) COMMENT '保险单照片路径',
    current_destination VARCHAR(255) COMMENT '当前借用去向',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_plate (plate_number),
    INDEX idx_status (status)
) COMMENT '车辆档案表';

-- 驾驶人信息表
CREATE TABLE IF NOT EXISTS driver (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT COMMENT '关联用户ID',
    driver_name VARCHAR(50) NOT NULL COMMENT '姓名',
    license_number VARCHAR(50) NOT NULL UNIQUE COMMENT '驾驶证号',
    license_type VARCHAR(20) NOT NULL COMMENT '准驾车型',
    drive_age INT COMMENT '驾龄(年)',
    license_photo VARCHAR(255) COMMENT '驾照照片路径',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE-在职, LEAVED-离职',
    dept_id BIGINT COMMENT '所属部门',
    phone VARCHAR(20) COMMENT '联系电话',
    sort_order INT COMMENT '登录排序，数字越小越靠前',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_license (license_number),
    INDEX idx_user (user_id)
) COMMENT '驾驶人信息表';

-- 系统参数配置表
CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(50) NOT NULL UNIQUE COMMENT '参数键',
    config_value VARCHAR(255) NOT NULL COMMENT '参数值',
    config_desc VARCHAR(255) COMMENT '参数描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_key (config_key)
) COMMENT '系统参数配置表';

-- 陕西省内城市距离配置表
CREATE TABLE IF NOT EXISTS city_distance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_city VARCHAR(50) NOT NULL COMMENT '出发城市',
    to_city VARCHAR(50) NOT NULL COMMENT '目的城市',
    distance DECIMAL(10,2) NOT NULL COMMENT '距离(公里)',
    UNIQUE KEY uk_route (from_city, to_city)
) COMMENT '城市间距离配置表';

-- ===================== 借还车与调度管理 =====================

-- 借还车记录表
CREATE TABLE IF NOT EXISTS vehicle_borrow_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_no VARCHAR(50) NOT NULL UNIQUE COMMENT '借还车单号',
    vehicle_id BIGINT NOT NULL COMMENT '车辆ID',
    plate_number VARCHAR(20) NOT NULL COMMENT '借用时车牌号',
    driver_id BIGINT NOT NULL COMMENT '驾驶员ID',
    driver_name VARCHAR(50) NOT NULL COMMENT '驾驶员姓名',
    status VARCHAR(20) DEFAULT 'TAKEN' COMMENT '状态: TAKEN-已取车, RETURNED-已还车',
    usage_reason VARCHAR(500) COMMENT '用车事由',
    destination VARCHAR(255) COMMENT '目的地/去向',
    expected_return_time DATETIME COMMENT '预计还车时间',
    take_mileage DECIMAL(10,2) NOT NULL COMMENT '取车时里程',
    take_vehicle_photos TEXT COMMENT '取车车辆照片',
    take_mileage_photo VARCHAR(500) COMMENT '取车公里数照片',
    take_time DATETIME NOT NULL COMMENT '取车时间',
    return_mileage DECIMAL(10,2) COMMENT '还车时累计里程',
    return_vehicle_photos TEXT COMMENT '还车停车照片',
    return_mileage_photo VARCHAR(500) COMMENT '还车公里数照片',
    return_fuel_photo VARCHAR(500) COMMENT '还车油表照片',
    is_clean TINYINT DEFAULT 1 COMMENT '车辆是否干净: 1-是, 0-否',
    is_fuel_enough TINYINT DEFAULT 1 COMMENT '油量是否不少于半箱: 1-是, 0-否',
    issue_description VARCHAR(500) COMMENT '车辆异常说明',
    issue_photos TEXT COMMENT '车辆异常照片',
    action_required VARCHAR(255) COMMENT '需补充处理事项',
    follow_up_status VARCHAR(20) DEFAULT 'NONE' COMMENT '跟进状态: NONE-无需处理, PENDING-待处理, COMPLETED-已处理',
    follow_up_remark VARCHAR(500) COMMENT '跟进处理说明',
    follow_up_handled_by BIGINT COMMENT '跟进处理人',
    follow_up_handled_time DATETIME COMMENT '跟进处理时间',
    follow_up_result_status VARCHAR(20) COMMENT '处理后车辆状态',
    return_time DATETIME COMMENT '还车时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_borrow_vehicle (vehicle_id),
    INDEX idx_borrow_driver (driver_id),
    INDEX idx_borrow_status (status),
    INDEX idx_borrow_take_time (take_time)
) COMMENT '借还车记录表';

-- 借还车记录修改日志表
CREATE TABLE IF NOT EXISTS vehicle_borrow_record_edit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id BIGINT NOT NULL COMMENT '借还车记录ID',
    record_no VARCHAR(50) NOT NULL COMMENT '借还车单号',
    operator_id BIGINT COMMENT '修改人ID',
    operator_name VARCHAR(100) COMMENT '修改人名称',
    change_summary VARCHAR(1000) COMMENT '修改摘要',
    before_snapshot TEXT COMMENT '修改前快照',
    after_snapshot TEXT COMMENT '修改后快照',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_borrow_record_edit_record (record_id),
    INDEX idx_borrow_record_edit_time (create_time)
) COMMENT '借还车记录修改日志表';

-- ===================== 加油管理 =====================
-- 加油记录表
CREATE TABLE IF NOT EXISTS fuel_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vehicle_id BIGINT NOT NULL COMMENT '车辆ID',
    driver_id BIGINT COMMENT '驾驶人ID',
    fuel_type VARCHAR(20) DEFAULT '92#' COMMENT '油品类型',
    fuel_amount DECIMAL(8,2) NOT NULL COMMENT '加油量(升)',
    fuel_price DECIMAL(8,2) NOT NULL COMMENT '单价(元/升)',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '总金额(元)',
    fuel_date DATETIME NOT NULL COMMENT '加油时间',
    fuel_location VARCHAR(100) COMMENT '加油站',
    is_cash TINYINT DEFAULT 1 COMMENT '是否现金加油: 1-现金加油',
    cash_photo VARCHAR(255) COMMENT '现金加油小票照片',
    cash_reason VARCHAR(500) COMMENT '现金加油原因',
    leader_approval_photo VARCHAR(500) COMMENT '领导同意截图',
    fuel_gauge_photo VARCHAR(500) COMMENT '加油后油表照片',
    is_fuel_enough_after_fuel TINYINT DEFAULT 1 COMMENT '加油后是否不少于半箱: 1-是, 0-否',
    budget_year INT COMMENT '预算年度',
    cash_report_status VARCHAR(20) DEFAULT 'NONE' COMMENT '现金加油报备状态: NONE-无需报备, PENDING-待审批, APPROVED-已批准, REJECTED-已驳回',
    cash_report_approve_by BIGINT COMMENT '报备审批人ID',
    cash_report_approve_time DATETIME COMMENT '报备审批时间',
    invoice_photo VARCHAR(255) COMMENT '发票照片',
    remark VARCHAR(500) COMMENT '备注',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_vehicle (vehicle_id),
    INDEX idx_fuel_date (fuel_date),
    INDEX idx_is_cash (is_cash)
) COMMENT '加油记录表';

-- ===================== 维修保养管理 =====================
-- 维修保养申请表
CREATE TABLE IF NOT EXISTS maintenance_apply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    apply_no VARCHAR(50) NOT NULL UNIQUE COMMENT '申请单号',
    vehicle_id BIGINT NOT NULL COMMENT '车辆ID',
    apply_type VARCHAR(20) NOT NULL COMMENT '申请类型: MAINTENANCE-保养, REPAIR-维修',
    fault_description VARCHAR(500) COMMENT '故障/保养项目描述',
    estimated_cost DECIMAL(10,2) COMMENT '预估费用',
    repair_factory VARCHAR(100) COMMENT '维修厂',
    inquiry_result VARCHAR(500) COMMENT '询价结果',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING-待审批, APPROVED-已批准, REJECTED-已驳回, COMPLETED-已完成',
    is_large_amount TINYINT DEFAULT 0 COMMENT '是否大额维修(>5000元): 0-否, 1-是',
    approver_id BIGINT COMMENT '审批人ID(公司领导)',
    approve_time DATETIME COMMENT '审批时间',
    approve_comment VARCHAR(500) COMMENT '审批意见',
    applicant_id BIGINT COMMENT '申请人ID(综合办公室)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_apply_no (apply_no),
    INDEX idx_vehicle (vehicle_id),
    INDEX idx_status (status)
) COMMENT '维修保养申请表';

-- 维修保养记录表
CREATE TABLE IF NOT EXISTS maintenance_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    apply_id BIGINT COMMENT '关联申请ID',
    vehicle_id BIGINT NOT NULL COMMENT '车辆ID',
    maintenance_type VARCHAR(20) NOT NULL COMMENT '维保类型',
    maintenance_date DATE NOT NULL COMMENT '维修/保养日期',
    repair_factory VARCHAR(100) NOT NULL COMMENT '维修厂',
    send_person_id BIGINT COMMENT '送修人ID',
    maintenance_items TEXT COMMENT '维修/保养项目(JSON)',
    actual_cost DECIMAL(10,2) NOT NULL COMMENT '实际费用',
    mileage_after DECIMAL(10,2) COMMENT '维修后里程',
    receipt_photo VARCHAR(255) COMMENT '维修单据照片',
    acceptance_result VARCHAR(500) COMMENT '验收结果',
    accept_by BIGINT COMMENT '验收人ID',
    accept_time DATETIME COMMENT '验收时间',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_vehicle (vehicle_id),
    INDEX idx_maintenance_date (maintenance_date)
) COMMENT '维修保养记录表';

-- 维修工单表（新版）
CREATE TABLE IF NOT EXISTS maintenance_work_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '工单号',
    vehicle_id BIGINT NOT NULL COMMENT '车辆ID',
    plate_number_snapshot VARCHAR(20) COMMENT '创建时车牌快照',
    source_type VARCHAR(50) COMMENT '来源类型: MANUAL, BORROW_RECORD 等',
    source_record_id BIGINT COMMENT '来源记录ID',
    work_type VARCHAR(50) COMMENT '维保类型: REPAIR/MAINTENANCE 等',
    status VARCHAR(30) NOT NULL COMMENT '状态: DRAFT, PENDING_APPROVAL, APPROVED, IN_REPAIR, WAIT_ACCEPTANCE, COMPLETED, REJECTED, CANCELED',
    issue_description VARCHAR(1000) COMMENT '问题描述',
    issue_photos TEXT COMMENT '问题照片CSV',
    reported_by BIGINT COMMENT '上报人',
    reported_time DATETIME COMMENT '上报时间',
    repair_vendor VARCHAR(100) COMMENT '维修厂',
    repair_contact VARCHAR(100) COMMENT '维修联系人',
    estimated_cost DECIMAL(10,2) COMMENT '预估费用',
    actual_cost DECIMAL(10,2) COMMENT '实际费用',
    planned_start_time DATETIME COMMENT '计划开始',
    expected_finish_time DATETIME COMMENT '预计完工时间',
    repair_start_time DATETIME COMMENT '开工时间',
    repair_finish_time DATETIME COMMENT '完工时间',
    accepted_by BIGINT COMMENT '验收人',
    accepted_time DATETIME COMMENT '验收时间',
    acceptance_result VARCHAR(500) COMMENT '验收结果',
    close_result_status VARCHAR(50) COMMENT '关闭结论',
    remark VARCHAR(1000) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_mwo_vehicle (vehicle_id),
    INDEX idx_mwo_status (status),
    INDEX idx_mwo_source (source_type, source_record_id)
) COMMENT '维修工单表';

-- 维修工单附件表
CREATE TABLE IF NOT EXISTS maintenance_work_order_attachment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT '工单ID',
    category VARCHAR(50) NOT NULL COMMENT '附件类别: ISSUE/QUOTE/APPROVAL/REPAIR/INVOICE/ACCEPTANCE',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_mwo_att_order (order_id),
    INDEX idx_mwo_att_category (category)
) COMMENT '维修工单附件表';

-- ===================== 保险与年检管理 =====================
-- 保险记录表
CREATE TABLE IF NOT EXISTS insurance_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vehicle_id BIGINT NOT NULL COMMENT '车辆ID',
    insurance_type VARCHAR(50) NOT NULL COMMENT '保险类型: 交强险, 商业险',
    insurance_company VARCHAR(100) NOT NULL COMMENT '保险公司',
    premium DECIMAL(10,2) NOT NULL COMMENT '保费金额',
    start_date DATE NOT NULL COMMENT '保险起始日期',
    end_date DATE NOT NULL COMMENT '保险截止日期',
    policy_number VARCHAR(50) COMMENT '保单号',
    photo VARCHAR(255) COMMENT '保险单照片',
    operator_id BIGINT COMMENT '办理人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_vehicle (vehicle_id),
    INDEX idx_end_date (end_date)
) COMMENT '保险记录表';

-- 年检记录表
CREATE TABLE IF NOT EXISTS inspection_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vehicle_id BIGINT NOT NULL COMMENT '车辆ID',
    inspection_type VARCHAR(20) DEFAULT '年度检' COMMENT '年检类型',
    inspection_date DATE NOT NULL COMMENT '年检日期',
    next_inspection_date DATE COMMENT '下次年检日期',
    inspection_result VARCHAR(20) DEFAULT '合格' COMMENT '年检结果',
    certificate_number VARCHAR(50) COMMENT '检验合格证号',
    photo VARCHAR(255) COMMENT '年检证书照片',
    operator_id BIGINT COMMENT '办理人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_vehicle (vehicle_id),
    INDEX idx_next_date (next_inspection_date)
) COMMENT '年检记录表';

-- 出险记录表
CREATE TABLE IF NOT EXISTS accident_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vehicle_id BIGINT NOT NULL COMMENT '车辆ID',
    apply_id BIGINT COMMENT '关联用车申请ID',
    accident_time DATETIME NOT NULL COMMENT '事故时间',
    accident_location VARCHAR(100) NOT NULL COMMENT '事故地点',
    accident_reason VARCHAR(500) COMMENT '事故原因',
    accident_type VARCHAR(20) COMMENT '事故类型',
    liability_type VARCHAR(20) COMMENT '责任类型: 全责, 主责, 同责, 次责, 无责',
    driver_id BIGINT COMMENT '驾驶人ID',
    loss_amount DECIMAL(10,2) COMMENT '损失金额',
    claim_amount DECIMAL(10,2) COMMENT '理赔金额',
    claim_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '理赔状态: PENDING-待理赔, PROCESSING-理赔中, COMPLETED-已完成, REJECTED-拒赔',
    claim_company VARCHAR(100) COMMENT '理赔公司',
    claim_photo VARCHAR(255) COMMENT '事故现场照片',
    handle_result VARCHAR(500) COMMENT '处理结果',
    driver_responsibility DECIMAL(10,2) COMMENT '驾驶人承担金额',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_vehicle (vehicle_id),
    INDEX idx_accident_time (accident_time)
) COMMENT '出险记录表';

-- ===================== 违规与事故处理 =====================
-- 违章记录表
CREATE TABLE IF NOT EXISTS violation_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vehicle_id BIGINT NOT NULL COMMENT '车辆ID',
    driver_id BIGINT COMMENT '责任驾驶人ID',
    apply_id BIGINT COMMENT '关联用车申请ID',
    violation_time DATETIME NOT NULL COMMENT '违章时间',
    violation_location VARCHAR(100) NOT NULL COMMENT '违章地点',
    violation_type VARCHAR(50) COMMENT '违章类型',
    violation_content VARCHAR(200) COMMENT '违章内容',
    penalty_amount DECIMAL(10,2) COMMENT '罚款金额',
    penalty_points INT COMMENT '扣分',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '处理状态: PENDING-未处理, PROCESSING-处理中, COMPLETED-已处理',
    handle_deadline DATE COMMENT '处理期限',
    handle_time DATETIME COMMENT '处理时间',
    handle_photo VARCHAR(255) COMMENT '处理凭证照片',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_vehicle (vehicle_id),
    INDEX idx_driver (driver_id),
    INDEX idx_status (status)
) COMMENT '违章记录表';

-- 事故记录表(详细)
CREATE TABLE IF NOT EXISTS accident_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vehicle_id BIGINT NOT NULL COMMENT '车辆ID',
    driver_id BIGINT NOT NULL COMMENT '驾驶人ID',
    apply_id BIGINT COMMENT '关联用车申请ID',
    accident_time DATETIME NOT NULL COMMENT '事故时间',
    accident_location VARCHAR(100) COMMENT '事故地点',
    accident_cause VARCHAR(500) COMMENT '事故原因',
    responsibility VARCHAR(20) COMMENT '责任划分',
    loss_description VARCHAR(500) COMMENT '损失描述',
    loss_amount DECIMAL(10,2) COMMENT '损失金额',
    compensation_type VARCHAR(20) COMMENT '赔偿方式',
    compensation_amount DECIMAL(10,2) COMMENT '赔偿金额',
    handle_result VARCHAR(500) COMMENT '处理结果',
    scene_photos VARCHAR(500) COMMENT '现场照片(JSON数组)',
    responsibility_photo VARCHAR(255) COMMENT '责任认定书照片',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '事故详细记录表';

-- 罚款记录表
CREATE TABLE IF NOT EXISTS fine_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vehicle_id BIGINT COMMENT '关联车辆ID',
    driver_id BIGINT COMMENT '责任人ID',
    fine_type VARCHAR(50) NOT NULL COMMENT '罚款类型: 遗失证件, 遗失钥匙, 未按时保养, 违规停放, 其他',
    fine_amount DECIMAL(10,2) NOT NULL COMMENT '罚款金额',
    fine_reason VARCHAR(500) NOT NULL COMMENT '罚款原因',
    fine_date DATE NOT NULL COMMENT '罚款日期',
    pay_status VARCHAR(20) DEFAULT 'UNPAID' COMMENT '缴纳状态: UNPAID-未缴纳, PAID-已缴纳',
    pay_time DATETIME COMMENT '缴纳时间',
    pay_photo VARCHAR(255) COMMENT '缴纳凭证',
    operator_id BIGINT COMMENT '录入人ID',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_driver (driver_id),
    INDEX idx_pay_status (pay_status)
) COMMENT '罚款记录表';

-- ===================== 费用报销 =====================
-- 费用报销申请表
CREATE TABLE IF NOT EXISTS expense_apply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    apply_no VARCHAR(50) NOT NULL UNIQUE COMMENT '报销单号',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    apply_type VARCHAR(20) NOT NULL COMMENT '报销类型: CASH_FUEL-现金加油, PARKING-停车费, TOLL-高速费, PRIVATE_CAR-私车公用',
    apply_id BIGINT COMMENT '关联业务ID(用车申请/加油记录等)',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '报销金额',
    detail_items TEXT COMMENT '明细项目(JSON)',
    receipt_photos VARCHAR(500) COMMENT '票据照片(JSON数组)',
    status VARCHAR(20) DEFAULT 'PENDING_OFFICE' COMMENT '状态: PENDING_OFFICE-待办公室核销, PENDING_FINANCE-待财务审核, APPROVED-已通过, REJECTED-已驳回, PAID-已支付',
    office_verify_by BIGINT COMMENT '办公室核销人ID',
    office_verify_time DATETIME COMMENT '办公室核销时间',
    office_verify_comment VARCHAR(500) COMMENT '办公室核销意见',
    finance_verify_by BIGINT COMMENT '财务审核人ID',
    finance_verify_time DATETIME COMMENT '财务审核时间',
    finance_verify_comment VARCHAR(500) COMMENT '财务审核意见',
    private_car_type VARCHAR(20) COMMENT '私车公用补贴类型: FIXED-固定补贴, MILEAGE-里程补贴',
    mileage_subsidy DECIMAL(10,2) COMMENT '里程补贴金额',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_apply_no (apply_no),
    INDEX idx_user (user_id),
    INDEX idx_status (status)
) COMMENT '费用报销申请表';

-- 私车公用补贴配置表
CREATE TABLE IF NOT EXISTS private_car_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    subsidy_type VARCHAR(20) NOT NULL COMMENT '补贴类型: FIXED-固定补贴, MILEAGE-里程补贴',
    fixed_amount DECIMAL(10,2) COMMENT '固定补贴金额(元/月)',
    mileage_rate DECIMAL(5,2) COMMENT '里程补贴费率(元/公里)',
    start_date DATE COMMENT '生效开始日期',
    end_date DATE COMMENT '生效结束日期',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) COMMENT '私车公用补贴配置表';

-- ===================== 提醒与通知 =====================
-- 系统提醒记录表
CREATE TABLE IF NOT EXISTS system_reminder (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reminder_type VARCHAR(50) NOT NULL COMMENT '提醒类型: INSURANCE_EXPIRE-保险到期, INSPECTION_EXPIRE-年检到期, MAINTENANCE_MILEAGE-保养里程, FUEL_BUDGET-燃油预算, VIOLATION_PENDING-违章未处理, EXPENSE_PENDING-报销待审核',
    vehicle_id BIGINT COMMENT '关联车辆ID',
    user_id BIGINT COMMENT '关联用户ID',
    title VARCHAR(100) NOT NULL COMMENT '提醒标题',
    content VARCHAR(500) NOT NULL COMMENT '提醒内容',
    related_id BIGINT COMMENT '关联业务ID',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读: 0-未读, 1-已读',
    read_time DATETIME COMMENT '阅读时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_type (reminder_type),
    INDEX idx_vehicle (vehicle_id),
    INDEX idx_user (user_id),
    INDEX idx_is_read (is_read)
) COMMENT '系统提醒记录表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT COMMENT '操作人ID',
    operation_type VARCHAR(50) COMMENT '操作类型',
    operation_module VARCHAR(50) COMMENT '操作模块',
    operation_desc VARCHAR(200) COMMENT '操作描述',
    request_url VARCHAR(200) COMMENT '请求URL',
    request_method VARCHAR(10) COMMENT '请求方法',
    request_params TEXT COMMENT '请求参数',
    response_result TEXT COMMENT '返回结果',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_create_time (create_time)
) COMMENT '操作日志表';

-- ===================== 初始化数据 =====================
-- 初始化系统参数
INSERT INTO sys_config (config_key, config_value, config_desc) VALUES
('maintenance_mileage_threshold', '5000', '保养里程阈值(公里)'),
('maintenance_mileage_deviation', '200', '保养里程偏差(公里)'),
('annual_fuel_budget', '12000', '单车辆年燃油预算(元)'),
('private_car_subsidy_km', '1.2', '私车公用补贴(元/公里)'),
('fuel_consumption_rate', '10', '百公里油耗(升)'),
('low_fuel_threshold', '50', '低油量警告阈值(%)'),
('large_repair_threshold', '5000', '大额维修阈值(元)'),
('reminder_days_before', '30', '保险/年检提前提醒天数');

-- 初始化陕西省内城市距离
INSERT INTO city_distance (from_city, to_city, distance) VALUES
('西安', '咸阳', 30), ('西安', '宝鸡', 170), ('西安', '渭南', 60), ('西安', '铜川', 70), ('西安', '延安', 300),
('西安', '榆林', 600), ('西安', '汉中', 350), ('西安', '安康', 220), ('西安', '商洛', 120),
('咸阳', '宝鸡', 150), ('咸阳', '渭南', 80), ('咸阳', '铜川', 90),
('渭南', '铜川', 80), ('宝鸡', '汉中', 200);

-- 初始化演示用户（上线前请通过 INITIAL_ADMIN_PASSWORD 环境变量设置管理员初始密码）
INSERT INTO sys_user (username, password, real_name, phone, role, status) VALUES
('admin', '', '系统管理员', '13800000000', 'SUPER_ADMIN', 1),
('office_admin', '', '综合办公室', '13800000001', 'OFFICE_ADMIN', 1),
('dept_approver', '', '部门审批人', '13800000002', 'DEPT_APPROVER', 1),
('driver1', '', '张三', '13800000003', 'DRIVER', 1),
('driver2', '', '李四', '13800000004', 'DRIVER', 1),
('finance', '', '财务人员', '13800000005', 'FINANCE', 1);

-- 初始化部门
INSERT INTO sys_dept (dept_name, dept_code) VALUES
('综合办公室', 'ADMIN'), ('财务部', 'FINANCE'), ('市场部', 'MARKET'), ('技术部', 'TECH');

-- 初始化车辆数据
INSERT INTO vehicle (plate_number, vehicle_type, brand, model, color, vin, engine_number, register_date, current_mileage, annual_fuel_budget, annual_fuel_used, status, parking_location, insurance_company, insurance_expire_date, inspection_expire_date) VALUES
('陕A12345', '轿车', '大众', '帕萨特', '黑色', 'LSVAB4185DE123456', 'ENG123456', '2020-01-15', 50000, 12000, 3500, 'NORMAL', '西京酒店停车场', '中国人保', '2026-12-31', '2026-06-30'),
('陕A67890', 'SUV', '丰田', '汉兰达', '白色', 'LSVAB4185DE654321', 'ENG654321', '2021-03-20', 35000, 12000, 2800, 'NORMAL', '西京酒店停车场', '平安保险', '2026-09-30', '2026-09-20'),
('陕AB8888', '商务车', '别克', 'GL8', '灰色', 'LSVAB4185DE888888', 'ENG888888', '2019-08-10', 80000, 12000, 4500, 'NORMAL', '西京酒店停车场', '太平洋保险', '2026-07-15', '2026-08-15');

-- 初始化驾驶人数据
INSERT INTO driver (user_id, driver_name, license_number, license_type, drive_age, dept_id, phone, status) VALUES
(4, '张三', '610102198501011234', 'C1', 10, 2, '13800000003', 'ACTIVE'),
(5, '李四', '610102199003052345', 'C1', 8, 3, '13800000004', 'ACTIVE');
