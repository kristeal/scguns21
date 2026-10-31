package top.kristeal.item.attachment;

import top.kristeal.item.attachment.impl.Magazine;

public interface IMagazine extends IAttachment<Magazine>
{
    /**
     * @return The type of this attachment
     */
    @Override
    default Type getType()
    {
        return Type.MAGAZINE;
    }
}
