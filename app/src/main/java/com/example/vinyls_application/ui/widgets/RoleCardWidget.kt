package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import com.example.vinyls_application.R
import com.example.vinyls_application.databinding.WidgetRoleCardBinding

class RoleCardWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = WidgetRoleCardBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = HORIZONTAL

        context.withStyledAttributes(attrs, R.styleable.RoleCardWidget) {
            val buttonText = getString(R.styleable.RoleCardWidget_customText)
            binding.ivRoleIcon.setImageDrawable(getDrawable(R.styleable.RoleCardWidget_customIcon))
            binding.txtRoleTitle.text = getString(R.styleable.RoleCardWidget_roleTitle)
            binding.txtRoleDescription.text = getString(R.styleable.RoleCardWidget_roleDescription)
            binding.btnRoleEnter.text = buttonText
            binding.btnRoleEnterPrimary.text = buttonText
            setPrimary(getBoolean(R.styleable.RoleCardWidget_isPrimary, false))
        }
    }

    fun setPrimary(isPrimary: Boolean) {
        binding.btnRoleEnter.isVisible = !isPrimary
        binding.btnRoleEnterPrimary.isVisible = isPrimary

        binding.ivRoleIcon.setBackgroundResource(
            if (isPrimary) R.drawable.bg_role_icon_primary else R.drawable.bg_role_icon
        )
        binding.ivRoleIcon.imageTintList = ColorStateList.valueOf(
            if (isPrimary) Color.WHITE else Color.parseColor("#7C4DFF")
        )
    }

    fun setOnEnterClickListener(listener: OnClickListener?) {
        setOnClickListener(listener)
        binding.btnRoleEnter.setOnClickListener(listener)
        binding.btnRoleEnterPrimary.setOnClickListener(listener)
    }
}