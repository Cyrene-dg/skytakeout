package com.sky.controller.admin;

import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.dto.DishDTO;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 菜品管理
 */
@RequestMapping("/admin/dish")
@RestController
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @PostMapping
    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品 {}", dishDTO);
        dishService.saveWithFlavor(dishDTO);
        //清理缓存数据
        String key = "dish_" + dishDTO.getCategoryId();
        redisTemplate.delete(key);
        return Result.success(dishDTO);
    }

    /**
     * 根据分类id查询列表
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    public Result<List<Dish>> list(Long categoryId) {
        log.info("查询菜品编号：{}",categoryId);
        List<Dish> list = dishService.list(categoryId);
        return Result.success(list);
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("分页查询：{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 菜品批量删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    public Result delete(@RequestParam List<Long> ids) {
        log.info("菜品批量删除：{}", ids);
        dishService.deleteBatch(ids);

        //将所有的菜品缓存数据清理掉，所有以dish_开头的key
        cleanCache("*dish_*");

        return Result.success();
    }

    /**
     * 根据id查询菜品
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<DishVO> getById(@PathVariable Long id) {
        log.info("根据id查询菜品：{}", id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 修改菜品
     *
     * @param dishDTO
     * @return
     */
    @PutMapping
    public Result update(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品：{}", dishDTO);
        dishService.updateWithFlavor(dishDTO);

        //将所有的菜品缓存数据清理掉，所有以dish_开头的key
        cleanCache("*dish_*");

        return Result.success();
    }

    /**
     * 菜品起售停售
     *
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售停售")
    public Result<String> startOrStop(@PathVariable Integer status, Long id) {
        dishService.startOrStop(status, id);

        //将所有的菜品缓存数据清理掉，所有以dish_开头的key
        cleanCache("*dish_*");

        return Result.success();
    }

//    /**
//     * 清理缓存数据
//     * @param pattern
//     */
//    private void cleanCache(String pattern){
//        Set keys = redisTemplate.keys(pattern);
//        redisTemplate.delete(keys);
//    }
    /**
     * 清理缓存数据（添加日志，明确执行结果）
     * @param pattern 缓存key匹配模式（如dish_*）
     */
    private void cleanCache(String pattern) {
        try {
            // 1. 打印“开始清理”日志，明确要清理的key模式
            log.info("开始清理缓存，匹配模式：{}", pattern);

            // 2. 查询所有匹配的缓存key
            Set<String> matchingKeys = redisTemplate.keys(pattern); // 注意泛型，避免unchecked警告

            // 3. 根据“是否有匹配key”分支处理，打印不同日志
            if (matchingKeys == null || matchingKeys.isEmpty()) {
                log.info("缓存清理完成：未找到匹配模式为[{}]的缓存key，无需清理", pattern);
                return;
            }

            // 4. 执行缓存删除，并打印删除的key数量和具体key（DEBUG级别，避免日志冗余）
            long deleteCount = redisTemplate.delete(matchingKeys);
            log.info("缓存清理成功：匹配模式[{}]，共清理{}个缓存key", pattern, deleteCount);
            log.debug("本次清理的具体缓存key：{}", matchingKeys); // DEBUG级：开发/测试时看具体key，生产可关闭

        } catch (Exception e) {
            // 5. 捕获异常并打印警告日志（避免缓存清理失败导致业务中断）
            log.warn("缓存清理失败，匹配模式：{}", pattern, e); // 异常信息通过e传递，便于排查
        }
    }


}
