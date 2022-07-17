package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增套餐
     *
     * @param setmealDto
     * @return
     */
    @PostMapping
    @CacheEvict(value = "setmealCache",allEntries = true)
    public R<String> save(@RequestBody SetmealDto setmealDto) {
        log.info("前端传入的参数是：{}", setmealDto.toString());
        setmealService.saveWithDish(setmealDto);
        return R.success("新增套餐成功!");
    }

    /**
     * 套餐分页展示
     *
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        //构建分页器
        Page<Setmeal> pageInfo = new Page<>(page, pageSize);
        Page<SetmealDto> setmealDtoPage = new Page<>();
        //构建条件器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        //条件筛选
        queryWrapper.like(StringUtils.isNotEmpty(name), Setmeal::getName, name);
        //根据更新时间倒序排序
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        setmealService.page(pageInfo, queryWrapper);
        //对象拷贝
        BeanUtils.copyProperties(pageInfo, setmealDtoPage, "records");
        List<Setmeal> records = pageInfo.getRecords();
        List<SetmealDto> list = records.stream().map(item -> {
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item, setmealDto);
            //获取套餐分类id
            Long categoryId = item.getCategoryId();
            //获取套餐名称
            Category category = categoryService.getById(categoryId);
            //把名称赋值给setmealDto的categoryName属性中
            if (category != null) {
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
        }).collect(Collectors.toList());
        setmealDtoPage.setRecords(list);
        return R.success(setmealDtoPage);
    }


    /**
     * 根据id查询套餐信息和对应的菜品信息
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<Setmeal> get(@PathVariable Long id) {
        log.info("前端传入的id为:{}", id);
        SetmealDto setmealDto = setmealService.getByIdWithsetmealDishes(id);
        return R.success(setmealDto);
    }

    /**
     * 修改套餐信息
     *
     * @param setmealDto
     * @return
     */
    @PutMapping
    @CacheEvict(value = "setmealCache",allEntries = true)
    public R<String> update(@RequestBody SetmealDto setmealDto) {
        log.info("前端传入的参数为:{}", setmealDto.toString());
        setmealService.updateWithSetmealDishes(setmealDto);
        return R.success("修改套餐信息成功!");
    }

    /**
     * 批量删除套餐信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @CacheEvict(value = "setmealCache",allEntries = true)
    public R<String> delete(@RequestParam List<Long> ids) {
        setmealService.deleteWithSetmealDishes(ids);
        return R.success("删除套餐成功!");
    }

    /**
     * 批量启售和停售
     *
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    @CacheEvict(value = "setmealCache",allEntries = true)
    public R<String> updateStatus(@PathVariable int status, @RequestParam List<Long> ids) {
        //构造条件构造器
        LambdaUpdateWrapper<Setmeal> updateWrapper = new LambdaUpdateWrapper<>();
        //添加过滤条件
        updateWrapper.set(Setmeal::getStatus, status).in(Setmeal::getId, ids);
        setmealService.update(updateWrapper);
        return R.success("批量操作成功!");
    }

    /**
     * 前端展示套餐信息
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    @Cacheable(value = "setmealCache",key = "#setmeal.categoryId + '_' + #setmeal.status")
    public R<List<Setmeal>> list(Setmeal setmeal) {
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus() != null, Setmeal::getStatus, setmeal.getStatus());
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        List<Setmeal> list = setmealService.list(queryWrapper);
        return R.success(list);
    }
}
