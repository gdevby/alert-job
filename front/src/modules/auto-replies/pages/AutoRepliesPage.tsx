import './autoRepliesPage.scss';

import { useState } from 'react';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import { AccountsTab } from '../components/AccountsTab';
import { TemplatesTab } from '../components/TemplatesTab';
import { PromptsTab } from '../components/PromptsTab';

const tabs = {
  Accounts: 'accounts',
  Templates: 'templates',
  Prompts: 'prompts',
} as const;

const AutoRepliesPage = () => {
  const [selectedTab, setSelectedTab] = useState<typeof tabs[keyof typeof tabs]>('accounts');

  const handleChange = (_: React.SyntheticEvent, newValue: typeof tabs[keyof typeof tabs]) => {
    setSelectedTab(newValue);
  };

  return <div className='auto-replies-page'>
    <div className='container'>
       <Tabs value={selectedTab} onChange={handleChange}>
          <Tab value={tabs.Accounts} label="Аккаунты" />
          <Tab value={tabs.Templates} label="Шаблоны" />
          <Tab value={tabs.Prompts} label="Промпты" />
        </Tabs>

        {selectedTab === 'accounts' && <AccountsTab />}
        {selectedTab === 'templates' && <TemplatesTab />}
        {selectedTab === 'prompts' && <PromptsTab />}
    </div>
  </div>
}

export default AutoRepliesPage